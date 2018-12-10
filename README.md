# Rundeck Vault Storage Plugin

## Purpose
This is a [Storage Backend](http://rundeck.org/docs/plugins-user-guide/storage-plugins.html) plugin for storing Key Store data in [Vault](https://www.vaultproject.io/).

## Installation
  * Download and start [Rundeck](http://rundeck.org/downloads.html). It will automatically create the necessary directories.
  * Clone this repository. Build using `gradle` wrapper:
    ```
      ./gradlew clean build
    ```
  * Drop `rundeck-vault-plugin-<version>.jar` to `libext/` under Rundeck installation directory.
  * Restart Rundeck.

## Configuration

Add the following settings on $RDECK_BASE/etc/rundeck-config.properties

```
rundeck.storage.provider.1.type=vault-storage
rundeck.storage.provider.1.path=keys
rundeck.storage.provider.1.config.prefix=rundeck
rundeck.storage.provider.1.config.secretBackend=secret
rundeck.storage.provider.1.config.address=$VAULT_URL
rundeck.storage.provider.1.config.token=$VAULT_TOKEN
```

For existing vault storage, probably you will need to remove the default `keys` path added by default for rundeck.
You can use these settings for an existing vault storage:

```
rundeck.storage.provider.1.type=vault-storage
rundeck.storage.provider.1.path=keys
rundeck.storage.provider.1.removePathPrefix=true
rundeck.storage.provider.1.config.prefix=someprefix
rundeck.storage.provider.1.config.secretBackend=mybackend
rundeck.storage.provider.1.config.address=$VAULT_URL
rundeck.storage.provider.1.config.token=$VAULT_TOKEN
rundeck.storage.provider.1.config.storageBehaviour=vault
```

## Minimal version requirements
  * Java 1.8
  * Rundeck 2.10.0
  * Vault 0.9.0

## Thanks
  * [BetterCloud/vault-java-driver](https://github.com/BetterCloud/vault-java-driver) made this possible.

## TODO
  * Integration tests
  * Automated auth token lease renewal
  * Storage converter plugin

