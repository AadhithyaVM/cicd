#!/bin/bash
# Runs ON the Jenkins VM.
# Usage: bash bootstrap.sh <k8s-private-ip> <git-repo-url> <branch>
# Expects ~/cia2-vars.json (admin password + registry credentials) and ~/.ssh/cia2-key.pem
set -euo pipefail
K8S_IP="$1"; REPO="$2"; BRANCH="${3:-main}"
export DEBIAN_FRONTEND=noninteractive

sudo apt-get update -y
sudo apt-get install -y ansible git
chmod 400 ~/.ssh/cia2-key.pem
chmod 600 ~/cia2-vars.json

rm -rf ~/cia2-cicd
git clone --branch "$BRANCH" "$REPO" ~/cia2-cicd
cd ~/cia2-cicd/ansible

cat > inventory.ini <<INV
[jenkins]
localhost ansible_connection=local

[k8s]
$K8S_IP

[all:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=/home/ubuntu/.ssh/cia2-key.pem
INV

echo "=== Ansible: Kubernetes node ==="
ansible-playbook -i inventory.ini setup-k8s.yml
echo "=== Ansible: Jenkins server ==="
ansible-playbook -i inventory.ini setup-jenkins.yml -e @/home/ubuntu/cia2-vars.json
shred -u ~/cia2-vars.json 2>/dev/null || rm -f ~/cia2-vars.json
echo "=== Bootstrap finished ==="
