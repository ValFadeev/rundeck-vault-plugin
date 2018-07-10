#!/usr/bin/env bash


echo "Writing vault plugin settings"

cat - >>$RDECK_BASE/server/config/rundeck-config.properties <<END
rundeck.storage.provider.1.type=vault-storage
rundeck.storage.provider.1.path=keys
rundeck.storage.provider.1.config.prefix=rundeck
rundeck.storage.provider.1.config.address=$VAULT_URL
rundeck.storage.provider.1.config.token=$VAULT_TOKEN

rundeck.storage.provider.1.config.maxRetries=2
rundeck.storage.provider.1.config.retryIntervalMilliseconds=100
rundeck.storage.provider.1.config.openTimeout=1
rundeck.storage.provider.1.config.readTimeout=5

END

if [ "$VAULT_BEHAVIOUR" = "vault" ] ; then
cat - >>$RDECK_BASE/server/config/rundeck-config.properties <<END
rundeck.storage.provider.1.config.storageBehaviour=vault

END
fi