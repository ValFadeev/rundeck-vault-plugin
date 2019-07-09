all:

#env vars
DOCKER_COMPOSE_SPEC="docker-compose.yaml"
build:
	set -e
	# re-build docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) build
	
start:
	# run docker
	docker-compose -f $(DOCKER_COMPOSE_SPEC) up -d
	
clean:
	# clean up docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) down --volumes --remove-orphans

build_and_start:
	set -e
	# re-build docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) build
	# clean up docker env
	docker-compose -f $(DOCKER_COMPOSE_SPEC) down --volumes --remove-orphans
	# run docker
	docker-compose -f $(DOCKER_COMPOSE_SPEC) up -d