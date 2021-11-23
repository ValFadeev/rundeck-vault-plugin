all:

#env vars
DOCKER_COMPOSE_SPEC ?= docker-compose.yaml
ENV_FILE ?= .env

build:
	set -e
	# re-build docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) --env-file $(ENV_FILE) build
	
start:
	# run docker
	docker-compose -f $(DOCKER_COMPOSE_SPEC) --env-file $(ENV_FILE) up -d

clean:
	# clean up docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) --env-file $(ENV_FILE) down --volumes --remove-orphans
	rm data/envs.txt

build_and_start:
	set -e
	# re-build docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) --env-file $(ENV_FILE) build
	# clean up docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) --env-file $(ENV_FILE) down --volumes --remove-orphans
	# run docker
	docker-compose -f $(DOCKER_COMPOSE_SPEC) --env-file $(ENV_FILE) up -d
