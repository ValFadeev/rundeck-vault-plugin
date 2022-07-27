#!/usr/bin/env  sh

#start vault

vault server -config=/vault/config -dev & > log.out

version=$(curl -s http://vault:8200/v1/sys/health |jq -r  .version)

    echo "************ creating test keys"

if (( $version > 1 )); then
    echo "Vault 1.x"
    vault kv put secret/app/simple.secret foo=world
    vault kv put secret/app/multiples name=admin password=admin server=rundeck
    vault kv put secret/app/folder/another.secret test=hello
    vault kv put secret/app/folder/multiple2 name=admin password=admin server=rundeck

    vault secrets enable -path=rundeck kv
    vault kv put rundeck/simple.secret foo=world
    vault kv put rundeck/multiples name=admin password=admin server=rundeck
    vault kv put rundeck/folder/another.secret test=hello
    vault kv put rundeck/folder/multiple2 name=admin password=admin server=rundeck

else
    echo "Vault 0.x"

    vault write secret/app/simple.secret foo=world
    vault write secret/app/multiples name=admin password=admin server=rundeck
    vault write secret/app/folder/another.secret test=hello
    vault write secret/app/folder/multiple2 name=admin password=admin server=rundeck


    echo "************ end"

fi
sleep 10



tail -f log.out
