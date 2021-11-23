#!/bin/bash

ls -lrt /home/rundeck/script

bash /home/rundeck/script/waitforfile.sh /home/rundeck/vault-envs/envs.txt 30 10

export $(xargs < /home/rundeck/vault-envs/envs.txt)

env | grep VAULT

sed -i "s/^\(rundeck\.storage\.provider\.1\.config\.approleId\s*=\s*\).*\$/\1$VAULT_ROLE_ID/" server/config/rundeck-config.properties
sed -i "s/^\(rundeck\.storage\.provider\.1\.config\.approleSecretId\s*=\s*\).*\$/\1$VAULT_SECRET_ID/" server/config/rundeck-config.properties


cat server/config/rundeck-config.properties

exec java \
    -XX:+UnlockExperimentalVMOptions \
    -Dlog4j.configurationFile="${HOME}/server/config/log4j2.properties" \
    -Dlogging.config="file:${HOME}/server/config/log4j2.properties" \
    -Dloginmodule.conf.name=jaas-loginmodule.conf \
    -Dloginmodule.name=rundeck \
    -Drundeck.jaaslogin=true \
    "${@}" \
    -jar rundeck.war
