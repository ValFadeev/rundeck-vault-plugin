# Running Rundeck with Vault 


## Set environment variables

```
export RUNDECK_VERSION=2.10.7
export VAULT_TOKEN=sometoken

```

## Build the image

```
docker-compose -f docker-compose-vault.yml build
```


## Run rundeck with vault

```
docker-compose -f docker-compose-vault.yml up -d
```


## Stop rundeck and vault

```
docker-compose -f docker-compose-vault.yml down --volumes --remove-orphans
```
