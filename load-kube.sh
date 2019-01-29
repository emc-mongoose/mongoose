#!/bin/bash

start() {
    kubectl create namespace mongoose
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
