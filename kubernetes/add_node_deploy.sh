#!/usr/bin/env bash

yum install -y kubelet kubeadm kubectl
systemctl enable kubelet && systemctl start kubelet && systemctl status kubelet

echo Environment="KUBELET_EXTRA_ARGS=--fail-swap-on=false" >> /etc/systemd/system/kubelet.service.d/10-kubeadm.conf
systemctl daemon-reload && systemctl restart kubelet
yum update

yes | kubeadm reset

#
# The "join_command" file must be added after initializing the master.
#

./join_command.sh
export KUBECONFIG=/etc/kubernetes/kubelet.conf
cat > /etc/cni/net.d/10-flannel.conflist <<EOF
{
    "name": "cbr0",
    "plugins": [
        {
            "type": "flannel",
            "delegate": {
                "hairpinMode": true,
                "isDefaultGateway": true
            }
        },
        {
            "type": "portmap",
            "capabilities": {
                "portMappings": true
            }
        }
    ]
}

EOF

# check
kubectl get nodes
