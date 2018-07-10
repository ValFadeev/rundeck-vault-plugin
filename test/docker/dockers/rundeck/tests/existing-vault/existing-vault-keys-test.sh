#!/usr/bin/env roundup
#
set -e
: ${RUNDECK_USER?"environment variable not set."}
: ${RUNDECK_PROJECT?"environment variable not set."}


# The Plan
# --------
describe "Another test with existing vaults secrects"


it_create_password_key() {

    bash -c "echo somepassword > vault.password"
    bash -c "rd keys create -t password -p keys/node/vault.password -f vault.password" > test.output

    # diff with expected
    cat >expected.output <<END
# Created: keys/node/vault.password [password]
END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

it_check_key_value_job() {

    # Run job
    JOBID="9a0a8668-9d8d-43c3-9f22-caa329af4fbb"
    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format xml -f test-job.xml"

    bash -c "rd run -i $JOBID -p $RUNDECK_PROJECT"

    cmdout=($(bash -c "rd executions follow  -e 1 | grep -v '^#' "))
    expout=($(curl -s -H "X-Vault-Token: $VAULT_TOKEN"  http://vault:8200/v1/secret/rundeck/keys/node/vault.password | jq .data.value))
    echo "${cmdout[@]}"
    if ! test ${#expout[*]} = ${#cmdout[*]}
    then
        echo "FAIL: command output did not contain ${#expout[*]} lines. Contained ${#cmdout[*]}: ${cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi

}

it_check_password() {

    bash -c "rd keys list -p keys/node | grep vault.password" > test.output

    # diff with expected
    cat >expected.output <<END
keys/node/vault.password [password]
END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi

}

it_update_password_key() {
    bash -c "echo newpassword > vault.password"
    bash -c "rd keys update -t password -p keys/node/vault.password -f vault.password" > test.output

    # diff with expected
    cat >expected.output <<END
# Updated: keys/node/vault.password [password]
END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

it_delete_password_key() {

    bash -c "rd keys delete -p keys/node/vault.password" > test.output

    # diff with expected
    cat >expected.output <<END
# Deleted: keys/node/vault.password
END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi

    cmdout=($(curl -s -H "X-Vault-Token: $VAULT_TOKEN"  http://vault:8200/v1/secret/rundeck/keys/node/vault.password > test.output))
    # diff with expected
    cat >expected.output <<END
{"errors":[]}
END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}


it_list_existing_keys() {

    bash -c "rd keys list -p keys/" > test.output

    # diff with expected
    cat >expected.output <<END
keys/
keys/folder/
keys/multiples/
keys/simple.secret [password]

END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi

}

it_list_multiple_secret() {

    bash -c "rd keys list -p keys/multiples/" > test.output

    # diff with expected
    cat >expected.output <<END
keys/multiples/
keys/multiples/name [password]
keys/multiples/password [password]
keys/multiples/server [password]

END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi

}

it_check_multiple_key_value_job() {

    # Run job
    JOBID="a674bd14-c8f4-42a3-b73a-02672ffee9bb"
    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format xml -f test-job-multiples-keys.xml"

    bash -c "rd run -i $JOBID -p $RUNDECK_PROJECT"

    cmdout=($(bash -c "rd executions follow  -e 2 | grep -v '^#' "))
    expout=($(curl -s -H "X-Vault-Token: $VAULT_TOKEN"  http://vault:8200/v1/secret/rundeck/keys/multiples | jq .data.password))
    echo "${cmdout[@]}"
    if ! test ${#expout[*]} = ${#cmdout[*]}
    then
        echo "FAIL: command output did not contain ${#expout[*]} lines. Contained ${#cmdout[*]}: ${cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi

}


it_add_value_multiples_key() {
    bash -c "echo newpassword > vault.password"
    bash -c "rd keys update -t password -p keys/multiples/newvalue -f vault.password" > test.output

    # diff with expected
    cat >expected.output <<END
# Updated: keys/multiples/newvalue [password]
END
    set +e
    diff expected.output test.output
    result=$?
    set -e
    cat test.output
    rm expected.output test.output #test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

