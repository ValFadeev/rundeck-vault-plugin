## Rundeck-Vault integration example 

This is a docker compose environment wih rundeck, mysql and vault

### Requirements

* Copy vault plugin to `docker/rundeck/plugins`

### Simple example with Vault OSS

* Build

```
make build
```

* Start

```
make start
```


* Stop

```
make clean
```


### Example with Vault Enterprise using approle

* Build

```
export DOCKER_COMPOSE_SPEC=docker-compose-approle.yaml 
export ENV_FILE=.env-enterprise 

make build
```

* Start

```
make start
```


* Stop

```
make clean
```

