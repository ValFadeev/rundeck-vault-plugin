#!/usr/bin/env  sh

#start vault
vault server -config=/vault/config -dev & > log.out

sleep 10

version=$(curl -s http://vault:8200/v1/sys/health |jq -r  .version)
echo "version $version"

echo "************ creating test keys"
if [[ "$version" = "1.1.3" ]] ; then
    echo "Vault 1.x"

    #create files
    echo "************ creating test keys (default)"
    vault kv put secret/rundeck/keys/simple.secret foo=world
    vault kv put secret/rundeck/keys/multiples name=admin password=admin server=rundeck
    vault kv put secret/rundeck/keys/folder/another.secret test=hello
    vault kv put secret/rundeck/keys/folder/multiple2 name=admin password=admin server=rundeck
    echo "************ end"


else
    echo "Vault 0.x"

    #create files
    echo "************ creating test keys (default)"
    vault write secret/rundeck/keys/simple.secret foo=world
    vault write secret/rundeck/keys/multiples name=admin password=admin server=rundeck
    vault write secret/rundeck/keys/folder/another.secret test=hello
    vault write secret/rundeck/keys/folder/multiple2 name=admin password=admin server=rundeck
    echo "************ end"

    #create files
    echo "************ creating custom backend keys"
    vault secrets enable -path=rundeckbackend kv
    vault write rundeckbackend/app/simple.secret foo=world
    vault write rundeckbackend/app/multiples name=admin password=admin server=rundeck
    vault write rundeckbackend/app/folder/another.secret test=hello
    vault write rundeckbackend/app/folder/multiple2 name=admin password=admin server=rundeck
    echo "************ end"

fi


tail -f log.out
