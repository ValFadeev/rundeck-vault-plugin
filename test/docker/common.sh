#!/bin/bash

export RUNDECK_VERSION=${RUNDECK_VERSION:-2.11.4}
export LAUNCHER_URL=${LAUNCHER_URL:-http://dl.bintray.com/rundeck/rundeck-maven/rundeck-launcher-${RUNDECK_VERSION}.jar}
export CLI_DEB_URL=${CLI_DEB_URL:-https://dl.bintray.com/rundeck/rundeck-deb}
export CLI_VERS=${CLI_VERS:-1.0.23-1}


build_rdtest_docker(){
	if [ -f rundeck-launcher.jar ] ; then
		mv rundeck-launcher.jar dockers/rundeck/data/
	fi

	# create base image for rundeck
	docker build \
		-t rdtest:latest \
		--build-arg LAUNCHER_URL=$LAUNCHER_URL \
		--build-arg CLI_DEB_URL=$CLI_DEB_URL \
		--build-arg CLI_VERS=$CLI_VERS \
		dockers/rundeck

}