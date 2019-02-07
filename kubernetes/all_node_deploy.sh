#!/usr/bin/env bash
# install wget or curl
yum install curl
# install unzip
yum install unzip
# copy CA
cp *.cer /etc/pki/ca-trust/source/anchors/CA.pem
cp *.cer /
# import certificate authority to the system
update-ca-trust

yum install docker
systemctl enable docker
systemctl start docker
systemctl restart docker

cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg
       https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
EOF

#Disable Firewall
systemctl disable firewalld
systemctl stop firewalld
iptables -L
echo 'net.bridge.bridge-nf-call-iptables = 1' > /etc/sysctl.d/87-sysctl.conf

#Disable SELinux:
setenforce 0
sed -i s/^SELINUX=.*$/SELINUX=disabled/ /etc/selinux/config

#Swap
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

reboot
