#!/bin/bash

NAMESPACE_NAME='namespace/mongoose'

create_ns_if_needed(){
	result=`kubectl get namespaces -o name | grep $NAMESPACE_NAME`
	if ! [[ $result == *$NAMESPACE_NAME* ]]
	then
		kubectl create namespace mongoose
	fi
}

start() {
    create_ns_if_needed
    kubectl create -f mongoose.yml
#    kubectl apply -f mongoose-controller.yaml
}

stop() {
   kubectl delete -f mongoose.yml
   #kubectl delete -f mongoose-driver.yaml
   kubectl delete namespace mongoose
}

log() {
  pod_name=`kubectl get pods -n mongoose | grep mongoose | awk '{print $1}'`
  kubectl logs -f -n mongoose $pod_name
}

ret=-1
if [[ -n "$1" ]]; then
    case "$1" in
        "--start")
            start
            ret=$?
        ;;
        "--stop")
            stop
            ret=$?
        ;;
        "--log")
            log
            ret=$?
        ;;
        *)
        echo "wrong input argument provided: '$1'"
        echo "available args: --start, --stop, --log"
        ret=5
        ;;
    esac
else
    echo "no input argument provided"
    echo "available args: --start, --stop, --log"
    ret=7
fi

exit ${ret}
