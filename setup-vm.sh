#!/bin/bash
set -e

echo "==> Updating packages..."
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg git

echo "==> Installing Docker..."
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "==> Adding $USER to docker group..."
sudo usermod -aG docker $USER

echo "==> Configuring vm.max_map_count for OpenSearch..."
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
sudo sysctl -w vm.max_map_count=262144

echo "==> Opening firewall ports..."
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 8180 -j ACCEPT

echo "==> Cloning repository..."
git clone https://github.com/YOUR_USERNAME/patient-system.git
cd patient-system

echo ""
echo "Done. Log out and SSH back in for docker group to take effect."
echo "Then run: cd patient-system && docker compose up -d"
