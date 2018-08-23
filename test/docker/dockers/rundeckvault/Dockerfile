FROM rundeck/rundeck:SNAPSHOT

USER root

RUN apt-get update && \
    apt-get -y install apt-transport-https curl && \
    apt-get -y install jq

# add cli tool debian repo
RUN echo "deb https://dl.bintray.com/rundeck/rundeck-deb /" | sudo tee -a /etc/apt/sources.list
RUN curl "https://bintray.com/user/downloadSubjectPublicKey?username=bintray" > /tmp/bintray.gpg.key
RUN apt-key add - < /tmp/bintray.gpg.key
RUN apt-get -y update
RUN apt-get -y install rundeck-cli

# RUNDECK
## RUNDECK setup env

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color


WORKDIR $HOME
USER rundeck

COPY --chown=rundeck:rundeck remco /etc/remco

# Copy files.
RUN sudo mkdir -p /tests
COPY tests /tests
RUN sudo chmod -R a+x /tests/*

RUN mkdir -p $HOME/vault-tests
COPY tests $HOME/vault-tests
RUN sudo chown -R rundeck:rundeck $HOME/vault-tests
RUN sudo chmod -R a+x $HOME/vault-tests/*

VOLUME $HOME/vault-tests

COPY --chown=rundeck:rundeck ./plugins ./libext


