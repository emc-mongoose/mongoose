#!/usr/bin/env bash

yum install -y kubelet kubeadm kubectl
systemctl enable kubelet && systemctl start kubelet && systemctl status kubelet

echo Environment="KUBELET_EXTRA_ARGS=--fail-swap-on=false" >> /etc/systemd/system/kubelet.service.d/10-kubeadm.conf
systemctl daemon-reload && systemctl restart kubelet
yum update

yes | kubeadm reset
kubeadm init --pod-network-cidr=10.244.0.0/16 --ignore-preflight-errors Swap | tee ./init_output
cat ./init_output |  grep 'kubeadm join ' > join_command.sh
chmod +x join_command.sh

mkdir -p $HOME/.kube
chown $(id -u):$(id -g) /etc/kubernetes/admin.conf
yes | cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
#chown $(id -u):$(id -g) $HOME/.kube/config
#kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml

kubectl taint nodes --all node-role.kubernetes.io/master-

#check
kubectl cluster-info
