#!/bin/bash

create_ns_if_needed(){
	result=`kubectl get namespaces -o name | grep 'namespace/mongoose'`
	if [[ -z result ]]
	then
		`kubectl create namespace mongoose`
	fi
}

start() {
    create_ns_if_needed
    echo "$1"
#    kubectl apply -f mongoose-driver.yaml
#    kubectl apply -f mongoose-controller.yaml
}

stop() {
   kubectl delete -f mongoose-controller.yaml
   kubectl delete -f mongoose-driver.yaml
   kubectl delete namespace mongoose
}

log() {
  pod_name=`kubectl get pods -n mongoose | grep controller | awk '{print $1}'`
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
        ret=5
        ;;
    esac
else
    echo "no input argument provided"
    ret=7
fi

exit ${ret}
