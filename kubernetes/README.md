# Kubernetes cluster deploying

All the necessary scripts are in the directory `kubernetes/`. You can not clone the whole project, and only download tar-archive:
```bash
wget https://gitlab.com/emcmongoose/mongoose/raw/SLTM-1226-kubernetes/kubernetes.tar.gz
tar -xzf kubernetes.tar.gz 
```

## Arhive content

The archive contains the following scripts:

| File name | Comments |
| ------ | ------ |
| `all_node_deploy.sh` | Pre-installation script. Must be run on **ALL** nodes. |
| `master_node_create.sh` | Pre-installation script for cluster inizialization. Must be run on **MASTER** node. |
| `add_node_create.sh` | Pre-installation script to add nodes to the cluster. Must be run on **ADDITIONAL** nodes. |
| `load-kube.sh` | Script for mongoose deploying. |
| `mongoose.yml` | Configuration for mongoose deployment. |

## CA

If the cluster is created on computers that require access certificates, place the unpacked certificate (*.cer) in the folder with the  all scripts and it will be applied at the pre-installation stage.

## Usage

*This usage is relevant for CentOS 7*

1.  Connect to the node (for example, with ssh) and run the `all_node_deploy.sh` script. Do this step with all the nodes that will be in the cluster. 
```bash
ssh USER@SERVER
...
cd /path/to/kupernetes-directory
./all_node_deploy.sh
```
2.  After successfully completing step 1, the machine should reboot. Therefore, you need to reconnect again.
3.  First you need to initialize the master. To do this, run the `master_node_create.sh` script. Upon successful cluster initialization, a `join_command.sh` file is generated that contains the data for connecting to the cluster.
```bash
./master_node_create.sh
```
4.  Copy the file to an additional node in the same directory as the rest of the scripts and connect to it.
```bash
scp ./join_command.sh USER@SERVER:/path/to/kubernetes-directory/join_command.sh
```
5.  Run the script `add_node_create.sh`
```bash
./add_node_create.sh
```

> If you have a such error msg:
> ```bash
> [join] FYI: You can look at this config file with 'kubectl -n kube-system get cm kubeadm-config -oyaml'
> unable to fetch the kubeadm-config ConfigMap: failed to get config map: Unauthorized
> ```
> This means that the token has expired. You need to re-create it with a command `kubeadm token create` on MASTER node. Then add this new token to the file `./join_command.sh` after `--token` and restart the script on ADDITIONAL node.


IN PROGRESS