#!/usr/bin/env  sh

#start vault

vault server -config=/vault/config -dev & > log.out

version=$(curl -s http://vault:8200/v1/sys/health |jq -r  .version)


# create namespace
vault namespace create rundeck
vault namespace create -namespace=rundeck demo
vault namespace create -namespace=rundeck other

export VAULT_NAMESPACE=rundeck

export VAULT_ROLE_NAME="rundeck"

#Add AppRole Auth Method
vault auth enable approle
#Create 'rundeck' policy
vault policy write ${VAULT_ROLE_NAME} -<<EOF
path "auth/approle/*" {
  capabilities = [ "create", "read", "update", "delete", "list" ]
}

path "secret/*"
{
  capabilities = ["create", "read", "update", "delete", "list", "sudo"]
}

path "demo/secret/*" {
  capabilities = [ "create", "read", "update", "delete", "list" ]
}

EOF
#Create 'rundeck' role
vault write auth/approle/role/${VAULT_ROLE_NAME} token_policies="${VAULT_ROLE_NAME}" token_ttl=5m secret_id_ttl=0 secret_id_num_uses=0 token_num_uses=50 token_period=0 token_type="service"

#Confirm Role Details
echo "+++ --- Role Details --- +++"
vault read auth/approle/role/${VAULT_ROLE_NAME}
#Display role id:
#vault read auth/approle/role/${VAULT_ROLE_NAME}/role-id
role_id=$(vault read --format=json auth/approle/role/${VAULT_ROLE_NAME}/role-id | jq -r '.data.role_id')
#Generate secret id:
secret_id=$(vault write --format=json -f auth/approle/role/${VAULT_ROLE_NAME}/secret-id | jq -r '.data.secret_id')

cat > /data/envs.txt << EOL
VAULT_ROLE_ID=$role_id
VAULT_SECRET_ID=$secret_id
EOL

cat /data/envs.txt

echo "ROLE ID: $role_id"
echo "SECRET ID: $secret_id"
export VAULT_NAMESPACE=rundeck/demo


echo "************ creating test keys"

if (( $version > 1 )); then
    vault secrets enable -version=2 -path=secret kv


    echo "Vault 1.x"
    vault kv put secret/app/simple.secret foo=world
    vault kv put secret/app/multiples name=admin password=admin server=rundeck
    vault kv put secret/app/folder/another.secret test=hello
    vault kv put secret/app/folder/multiple2 name=admin password=admin server=rundeck

else
    echo "Vault 0.x"
    vault secrets enable -version=1 -path=secret kv

    vault write secret/app/simple.secret foo=world
    vault write secret/app/multiples name=admin password=admin server=rundeck
    vault write secret/app/folder/another.secret test=hello
    vault write secret/app/folder/multiple2 name=admin password=admin server=rundeck

    echo "************ end"

fi
sleep 10

tail -f log.out
