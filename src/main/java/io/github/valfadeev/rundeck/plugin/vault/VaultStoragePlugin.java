package io.github.valfadeev.rundeck.plugin.vault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Configurable;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
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

    protected static final String VAULT_STORAGE_KEY = "data";
    protected static final String RUNDECK_DATA_TYPE = "Rundeck-data-type";
    protected static final String RUNDECK_KEY_TYPE = "Rundeck-key-type";
    protected static final String RUNDECK_CONTENT_MASK = "Rundeck-content-mask";
    protected static final String PRIVATE_KEY_MIME_TYPE = "application/octet-stream";
    protected static final String PUBLIC_KEY_MIME_TYPE = "application/pgp-keys";
    protected static final String PASSWORD_MIME_TYPE = "application/x-rundeck-data-password";

    public static final int MAX_GUARANTEED_VALIDITY_SECONDS = 60;

    private String vaultPrefix;
    private String vaultSecretBackend;
    private Logical vault;
    private int guaranteedTokenValidity;
    //if is true, objects will be saved with rundeck default headers behaivour
    private boolean rundeckObject=true;
    private VaultClientProvider clientProvider;
    private Vault vaultClient;


    @Override
    public Description getDescription() {
        return DescriptionProvider.getDescription();
    }

    @Override
    public void configure(Properties configuration) throws ConfigurationException {
        vaultPrefix = configuration.getProperty(VAULT_PREFIX);
        vaultSecretBackend = configuration.getProperty(VAULT_SECRET_BACKEND);
        clientProvider = getVaultClientProvider(configuration);
        loginVault(clientProvider);

        //check storage behaivour
        String storageBehaviour=configuration.getProperty(VAULT_STORAGE_BEHAVIOUR);
        if(storageBehaviour!=null && storageBehaviour.equals("vault")){
            rundeckObject=false;
        }

        guaranteedTokenValidity = calculateGuaranteedTokenValidity(configuration);
    }

    protected VaultClientProvider getVaultClientProvider(Properties configuration) {
        return new VaultClientProvider(configuration);
    }

    protected int calculateGuaranteedTokenValidity(Properties configuration) {
        return Integer.min(
                Integer.parseInt(configuration.getProperty(VAULT_MAX_RETRIES))
                        * (Integer.parseInt(configuration.getProperty(VAULT_READ_TIMEOUT))
                            + Integer.parseInt(configuration.getProperty(VAULT_OPEN_TIMEOUT))
                            + Integer.parseInt(configuration.getProperty(VAULT_RETRY_INTERVAL_MILLISECONDS)) / 1000),
                MAX_GUARANTEED_VALIDITY_SECONDS
        );
    }

    public static String getVaultPath(String rawPath, String vaultSecretBackend, String vaultPrefix) {
        String path= String.format("%s/%s/%s", vaultSecretBackend, vaultPrefix, rawPath);
        return path;
    }

    private boolean isDir(String key) {
        return key.endsWith("/");
    }

    protected void lookup(){
        try {
            if (vaultClient.auth().lookupSelf().getTTL() <= guaranteedTokenValidity) {
                loginVault(clientProvider);
            }
        } catch (VaultException e) {
            if(e.getHttpStatusCode() == 403){//try login again
                loginVault(clientProvider);
            } else {
                e.printStackTrace();
            }
        }
    }

    private void loginVault(VaultClientProvider provider){
        try {
            vaultClient = provider.getVaultClient();
            vault = vaultClient.logical();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private boolean isVaultDir(String key) {

        try{
            lookup();
            if(vault.list(getVaultPath(key,vaultSecretBackend,vaultPrefix)).getListData().size() > 0){
                return true;
            }else{
                if(!rundeckObject) {
                    //key/value with multiples keys
                    KeyObject object = this.getVaultObject(PathUtil.asPath(key));
                    if (!object.isRundeckObject() && object.isMultiplesKeys()) {
                        return true;
                    }
                }

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


        KeyObject object;
        if(event.equals("create")){
           if(rundeckObject){
               object=new RundeckKey(path);
           }else{
               object=new VaultKey(path,null);
           }
        }else{
           object = this.getVaultObject(path);
        }

        Map<String, Object> payload=object.saveResource(content,event,baoStream);

        try {
            lookup();
            return vault.write(getVaultPath(object.getPath().getPath(),vaultSecretBackend,vaultPrefix), payload);
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
        KeyObject object = this.getVaultObject(path);
        return loadResource(object,event);
    }

    private Resource<ResourceMeta> loadResource(KeyObject object, String event) {

        if(object.isError()){
            throw new StorageException(
                    String.format("Encountered error while reading data from Vault %s",
                                  object.getErrorMessage()),
                    StorageException.Event.valueOf(event.toUpperCase()),
                    object.getPath());
        }

        return object.loadResource();


    }

    private Set<Resource<ResourceMeta>> listResources(Path path, KeyType type) {
        List<String> response;

        try {
            lookup();
            response = vault.list(getVaultPath(path.getPath(),vaultSecretBackend,vaultPrefix)).getListData();

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

        //vault object with multiples keys
        boolean isKeyMultipleValues=false;
        KeyObject multivalueObject = null;
        if(filtered.isEmpty() && !rundeckObject){
            multivalueObject = this.getVaultObject(path);
            if(!multivalueObject.isRundeckObject() && multivalueObject.isMultiplesKeys()){
                isKeyMultipleValues=true;
                filtered = new ArrayList<>(multivalueObject.getKeys().keySet());
            }
        }

        for (String item : filtered) {

            Path itemPath = PathUtil.appendPath(path, item);

            Resource<ResourceMeta> resource=null;
            if (isDir(item)) {
                resource = loadDir(itemPath);
            } else {
                KeyObject object = this.getVaultObject(itemPath);

                if(rundeckObject){
                    //normal case with rundeck format
                    if(object.isRundeckObject()){
                         resource = loadResource(object,"list");
                    }
                }else{
                    //vault key/value format
                    if(isKeyMultipleValues){
                        //object with multiples keys
                        KeyObject multipleValue= new VaultKey(itemPath, item, multivalueObject.getKeys().get(item));
                        resource = loadResource(multipleValue,"list");
                    }else {
                        if(!object.isRundeckObject()){
                            if (object.isMultiplesKeys()) {
                                resource = loadDir(itemPath);
                            }else {
                                resource = loadResource(object,"list");
                            }
                        }
                    }
                }
            }

            if(resource!=null){
                resources.add(resource);
            }

        }

        return resources;
    }

    @Override
    public boolean hasPath(Path path) {
        try {
            lookup();
            if(vault.list(getVaultPath(path.getPath(),vaultSecretBackend,vaultPrefix)).getListData().size() > 0){
                return true;
            }

            KeyObject object = this.getVaultObject(path);
            if(!object.isRundeckObject()&& object.isMultiplesKeys()){
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
        KeyObject object=getVaultObject(path);

        if(object.isError()){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public boolean hasResource(String path) {
        return hasResource(PathUtil.asPath(path));
    }

    @Override
    public boolean hasDirectory(Path path) {
        try {
            lookup();
            List<String> list=vault.list(getVaultPath(path.getPath(),vaultSecretBackend,vaultPrefix)).getListData();

            if(list.size() > 0){
                return list.size() > 0;
            }else{
                KeyObject object = this.getVaultObject(path);
                if(object.isRundeckObject()==false && object.isMultiplesKeys()){
                    return true;
                }
            }

            return false;

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
        KeyObject object = this.getVaultObject(path);
        return object.delete(vault,vaultSecretBackend,vaultPrefix);
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

    public KeyObject getVaultObject(Path path){
        lookup();
        KeyObject value= KeyObjectBuilder.builder()
                                .path(path)
                                .vault(vault)
                                .vaultPrefix(vaultPrefix)
                                .vaultSecretBackend(vaultSecretBackend)
                                .build();

        return value;
    }



}