#!/bin/bash
set -e

# Fetch latest Cloudflare IPs
CF_IPS=$(curl -s https://www.cloudflare.com/ips-v4)

# Remove all existing Cloudflare rich rules
sudo firewall-cmd --permanent --get-rich-rules | tr ' ' '\n' | grep -o '".*"' | while read rule; do
  sudo firewall-cmd --permanent --remove-rich-rule="$rule" 2>/dev/null || true
done

# Add updated Cloudflare IPs
for ip in $CF_IPS; do
  sudo firewall-cmd --permanent --add-rich-rule="rule family='ipv4' source address='$ip' port port='80' protocol='tcp' accept"
  sudo firewall-cmd --permanent --add-rich-rule="rule family='ipv4' source address='$ip' port port='443' protocol='tcp' accept"
done

sudo firewall-cmd --reload

echo "Cloudflare IPs updated at $(date)"
