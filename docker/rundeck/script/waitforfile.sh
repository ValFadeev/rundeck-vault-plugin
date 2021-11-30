#!/bin/bash

syntax_error() { echo >&2 "SYNTAX: $*"; exit 2; }

(( $# != 3 )) && {
   syntax_error "$0 <file> <interval> <maxtry>"
}

declare -r FILE=$1 INTERVAL=$2 MAXTRY=$3

echo "waiting for ${FILE}"

progress_tic() { if [[ -t 1 ]]; then printf -- "%s" "$@"; else printf -- "%s\n" "$@" ;  fi ; }

echo "Running as user: $(whoami)"

declare -i attempts=0
while (( attempts <= MAXTRY ))
do
    if ! test -f "$FILE"
    then  progress_tic "."; # output a progress string.
    else  break; # file exists
    fi
    (( attempts += 1 ))  ; # increment attempts attemptser.
    (( attempts == MAXTRY )) && {
        echo "FAIL: Reached max try file exists: $FILE. Exiting."
        exit 1
    }
    sleep "$INTERVAL"; # wait before trying again.
done