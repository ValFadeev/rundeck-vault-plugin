#!/usr/bin/env  sh

#start vault
vault server -config=/vault/config -dev & > log.out

sleep 10

#create files
echo "************ creating test keys"
vault write secret/rundeck/keys/simple.secret foo=world
vault write secret/rundeck/keys/multiples name=admin password=admin server=rundeck
vault write secret/rundeck/keys/folder/another.secret test=hello
vault write secret/rundeck/keys/folder/multiple2 name=admin password=admin server=rundeck


echo "************ end"


tail -f log.out