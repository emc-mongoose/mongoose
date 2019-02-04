#!/usr/bin/env bash

# $1 - name of pkg
install(){
	pkg_manager=`./detect_pm.sh`
	yes | $pkg_manager $1
}

ssh_connect(){
	install 'sshpass'
	sshpass -p $1 ssh $2@$3 $4

}

ret=-1
if [[ -n "$1" ]]; then
	PASSWORD=abc123
	USER=root
	SERVER=$1
	SCRIPT="mkdir fsdgasdsjahdkjaskdhkajshdkjshdkjaskdhska"
	ssh_connect $PASSWORD $USER $SERVER $SCRIPT

#    case "$1" in
#        "--start")
#            start
#            ret=$?
#        ;;
#        "--stop")
#            stop
#            ret=$?
#        ;;
#        "--log")
#            log
#            ret=$?
#        ;;
#        *)
#        echo "wrong input argument provided: '$1'"
#        echo "available args: --start, --stop, --log"
#        ret=5
#        ;;
#    esac
else
    echo "no input argument provided"
#    echo "available args: --start, --stop, --log"
    echo "ip address needed"
    ret=7
fi

exit ${ret}
