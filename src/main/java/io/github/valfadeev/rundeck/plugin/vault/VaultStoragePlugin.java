package io.github.valfadeev.rundeck.plugin.vault;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Configurable;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.core.storage.StorageUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;
import org.rundeck.storage.impl.ResourceBase;

import static io.github.valfadeev.rundeck.plugin.vault.ConfigOptions.*;

/**
 * {@link VaultStoragePlugin} that stores Key Store data in Vault
 *
 * @author ValFadeev
 * @since 2017-09-18
 */

@Plugin(name = "vault-storage", service = ServiceNameConstants.Storage)
public class VaultStoragePlugin implements StoragePlugin, Configurable, Describable {

    java.util.logging.Logger log = java.util.logging.Logger.getLogger("vault-storage");

    public VaultStoragePlugin() {}

    private static final String VAULT_STORAGE_KEY = "data";
    private static final String RUNDECK_DATA_TYPE = "Rundeck-data-type";
    private static final String RUNDECK_KEY_TYPE = "Rundeck-key-type";
    private static final String RUNDECK_CONTENT_MASK = "Rundeck-content-mask";
    private static final String PRIVATE_KEY_MIME_TYPE = "application/octet-stream";
    private static final String PUBLIC_KEY_MIME_TYPE = "application/pgp-keys";
    private static final String PASSWORD_MIME_TYPE = "application/x-rundeck-data-password";


    private String vaultPrefix;
    private Logical vault;


    @Override
    public Description getDescription() {
        return DescriptionProvider.getDescription();
    }

    @Override
    public void configure(Properties configuration) throws ConfigurationException {
        vaultPrefix = configuration.getProperty(VAULT_PREFIX);
        vault = new VaultClientProvider(configuration)
                .getVaultClient()
                .logical();
    }

    private String getVaultPath(String rawPath) {
        return String.format("secret/%s/%s", vaultPrefix, rawPath);
    }

    private boolean isDir(String key) {
        return key.endsWith("/");
    }

    private boolean isVaultDir(String key) {
        try{
            if(vault.list(getVaultPath(key)).size() > 0){
                return true;
            }else{
                return false;
            }
        } catch (VaultException e) {
            log.info("error:" + e.getMessage());
            return false;
        }
    }

    private enum KeyType {
        RESOURCE,
        DIRECTORY,
        ALL
    }

    private VaultResponse saveResource(Path path, ResourceMeta content, String event) {
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        try {
            content.writeContent(baoStream);
        }
        catch (IOException e) {
            throw new StorageException(
                    String.format("Encountered error while extracting "
                                    + "resource content: %s",
                            e.getMessage()),
                    StorageException.Event.valueOf(event.toUpperCase()),
                    path);
        }

        Map<String, String> meta = content.getMeta();
        Map<String, Object> payload = new HashMap<>();
        for (String k : meta.keySet()) {
            payload.put(k, meta.get(k));
        }

        if (event.equals("update")) {
            DateFormat df = new SimpleDateFormat(StorageUtil.ISO_8601_FORMAT, Locale.ENGLISH);
            Resource<ResourceMeta> existing = loadResource(path, "write");
            payload.put(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME,
                    df.format(existing.getContents().getCreationTime()));
        }

        try {
            String data = baoStream.toString("UTF-8");
            payload.put(VAULT_STORAGE_KEY, data);
        } catch (UnsupportedEncodingException e) {
            throw new StorageException(
                    String.format("Encountered unsupported encoding error: %s",
                            e.getMessage()),
                    StorageException.Event.valueOf(event.toUpperCase()),
                    path);
        }

        try {
            return vault.write(getVaultPath(path.getPath()), payload);
        } catch (VaultException e) {
            throw new StorageException(
                    String.format("Encountered error while writing data to Vault %s",
                            e.getMessage()),
                    StorageException.Event.valueOf(event.toUpperCase()),
                    path);
        }
    }

    private Resource<ResourceMeta> loadDir(Path path) {
        return new ResourceBase<>(path, null, true);
    }

    private Resource<ResourceMeta> loadResource(Path path, String event) {
        LogicalResponse response;
        try {
            response = vault.read(getVaultPath(path.getPath()));
        } catch (VaultException e) {
            throw new StorageException(
            String.format("Encountered error while reading data from Vault %s",
                    e.getMessage()),
                    StorageException.Event.valueOf(event.toUpperCase()),
                    path);
        }

        Map<String, String> payload = response.getData();

        String data = payload.get(VAULT_STORAGE_KEY);

        ResourceMetaBuilder builder = new ResourceMetaBuilder();
        builder.setContentLength(Long.parseLong(payload.get(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH)));
        builder.setContentType(payload.get(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE));

        DateFormat df = new SimpleDateFormat(StorageUtil.ISO_8601_FORMAT, Locale.ENGLISH);
        try {
            builder.setCreationTime(df.parse(payload.get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME)));
            builder.setModificationTime(df.parse(payload.get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)));
        } catch (ParseException e) {
        }

        String type=payload.get(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE);
        if (type.equals(PRIVATE_KEY_MIME_TYPE)) {
            builder.setMeta(RUNDECK_CONTENT_MASK, "content");
            builder.setMeta(RUNDECK_KEY_TYPE, "private");
        }else if (type.equals(PUBLIC_KEY_MIME_TYPE)){
            builder.setMeta(RUNDECK_KEY_TYPE, "public");
        }else if (type.equals(PASSWORD_MIME_TYPE)){
            builder.setMeta(RUNDECK_CONTENT_MASK, "content");
            builder.setMeta(RUNDECK_DATA_TYPE, "password");
        }

        ByteArrayInputStream baiStream = new ByteArrayInputStream(data.getBytes());
        return new ResourceBase<>(path,
                StorageUtil.withStream(baiStream, builder.getResourceMeta()),
                false);
    }

    private Set<Resource<ResourceMeta>> listResources(Path path, KeyType type) {
        List<String> response;

        try {
            response = vault.list(getVaultPath(path.getPath()));
        } catch (VaultException e) {
            throw StorageException.listException(
                    path,
                    String.format("Encountered error while reading data from Vault %s",
                            e.getMessage()));
        }

        Set<Resource<ResourceMeta>> resources = new HashSet<>();

        List<String> filtered;
        if (type.equals(KeyType.RESOURCE)) {
            filtered = response.stream().filter(k -> !isDir(k)).collect(Collectors.toList());
        } else if (type.equals(KeyType.DIRECTORY)) {
            filtered = response.stream().filter(k -> isDir(k)).collect(Collectors.toList());
        } else {
            filtered = response;
        }

        for (String item : filtered) {
            Path itemPath = PathUtil.appendPath(path, item);
            Resource<ResourceMeta> resource;
            if (isDir(item)) {
                resource = loadDir(itemPath);
            } else {
                resource = loadResource(itemPath, "list");
            }
            resources.add(resource);
        }

        return resources;
    }

    @Override
    public boolean hasPath(Path path) {
        try {
            if(vault.list(getVaultPath(path.getPath())).size() > 0){
                return true;
            }

            //check if the path is a key
            return hasResource(path);
        } catch (VaultException e) {
            return false;
        }
    }

    @Override
    public boolean hasPath(String path) {
        return hasPath(PathUtil.asPath(path));
    }

    @Override
    public boolean hasResource(Path path) {
        try {
            return vault.read(getVaultPath(path.getPath())).getData().size() > 0;
        } catch (VaultException e) {
            return false;
        }
    }

    @Override
    public boolean hasResource(String path) {
        return hasResource(PathUtil.asPath(path));
    }

    @Override
    public boolean hasDirectory(Path path) {
        try {
            return vault.list(getVaultPath(path.getPath())).size() > 0;
        } catch (VaultException e) {
            return false;
        }
    }

    @Override
    public boolean hasDirectory(String path) {
        return hasDirectory(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> getPath(Path path) {
        if (isVaultDir(path.toString())) {
            return loadDir(path);
        } else {
            return loadResource(path, "read");
        }
    }

    @Override
    public Resource<ResourceMeta> getPath(String path) {
        return getPath(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> getResource(Path path) {
        return loadResource(path, "read");
    }

    @Override
    public Resource<ResourceMeta> getResource(String path) {
        return getResource(PathUtil.asPath(path));
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(Path path) {
        return listResources(path, KeyType.RESOURCE);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(String path) {
        return listDirectoryResources(PathUtil.asPath(path));
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(Path path) {
        return listResources(path, KeyType.ALL);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(String path) {
        return listDirectory(PathUtil.asPath(path));
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(Path path) {
        return listResources(path, KeyType.DIRECTORY);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(String path) {
        return listDirectoryResources(PathUtil.asPath(path));
    }

    @Override
    public boolean deleteResource(Path path) {
        try {
            vault.delete(getVaultPath(path.getPath()));
            return true;
        } catch (VaultException e) {
            return false;
        }
    }

    @Override
    public boolean deleteResource(String path) {
        return deleteResource(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        saveResource(path, content, "create");
        return loadResource(path, "read");
    }

    @Override
    public Resource<ResourceMeta> createResource(String path, ResourceMeta content) {
        return createResource(PathUtil.asPath(path), content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        saveResource(path, content, "update");
        return loadResource(path, "read");
    }

    @Override
    public Resource<ResourceMeta> updateResource(String path, ResourceMeta content) {
        return updateResource(PathUtil.asPath(path), content);
    }

}