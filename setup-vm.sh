#!/bin/bash
set -e

echo "==> Updating packages..."
dnf update -y
dnf install -y git dnf-plugins-core

echo "==> Installing Docker..."
dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "==> Starting Docker..."
systemctl start docker
systemctl enable docker

echo "==> Adding opc user to docker group..."
usermod -aG docker opc

echo "==> Configuring vm.max_map_count for OpenSearch..."
echo "vm.max_map_count=262144" | tee -a /etc/sysctl.conf
sysctl -w vm.max_map_count=262144

echo "==> Opening firewall ports..."
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --permanent --add-port=8180/tcp
firewall-cmd --reload

echo "==> Cloning repository..."
git clone https://github.com/stratidisgeorgios/Patient-Management-System.git /home/opc/patient-system
chown -R opc:opc /home/opc/patient-system

echo "==> Done. Log out, SSH back in, then run: cd patient-system && docker compose up -d"
