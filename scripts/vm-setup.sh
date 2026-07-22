#!/bin/bash
set -e

echo "==> Updating packages..."
apt-get update -y
apt-get upgrade -y
apt-get install -y git curl

echo "==> Installing Docker..."
curl -fsSL https://get.docker.com | sh

echo "==> Starting Docker..."
systemctl start docker
systemctl enable docker

echo "==> Adding ubuntu user to docker group..."
usermod -aG docker ubuntu

echo "==> Configuring vm.max_map_count for OpenSearch..."
echo "vm.max_map_count=262144" | tee -a /etc/sysctl.conf
sysctl -w vm.max_map_count=262144

echo "==> Cloning repository..."
git clone https://github.com/stratidisgeorgios/Patient-Management-System.git /home/ubuntu/patient-system
chown -R ubuntu:ubuntu /home/ubuntu/patient-system

echo "==> Done. Log out, SSH back in, then run: cd patient-system && docker compose up -d"
