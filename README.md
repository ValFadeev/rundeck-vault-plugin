# Rundeck Vault Storage Plugin

**Work in progress. Do not use in production!**

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

