FROM rdtest:latest


RUN mkdir -p $HOME/config
COPY config $HOME/config
VOLUME $HOME/config

RUN mkdir -p $HOME/vault-tests
COPY tests $HOME/vault-tests
RUN sudo chown -R rundeck:rundeck $HOME/vault-tests
RUN sudo chmod -R a+x $HOME/vault-tests/*

VOLUME $HOME/vault-tests

COPY plugins $HOME/libext

EXPOSE 4440