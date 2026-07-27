#!/bin/bash
# Run this on EC2 after terraform apply.
# Export the three vars from terraform outputs before calling:
#
#   export RDS_ADDRESS=<terraform output rds_address>
#   export KAFKA_BOOTSTRAP_SERVERS=<terraform output msk_bootstrap_brokers>
#   export OPENSEARCH_ENDPOINT=<terraform output opensearch_endpoint>
#   ./deploy.sh

set -euo pipefail

: "${RDS_ADDRESS?Set RDS_ADDRESS from terraform output rds_address}"
: "${KAFKA_BOOTSTRAP_SERVERS?Set KAFKA_BOOTSTRAP_SERVERS from terraform output msk_bootstrap_brokers}"
: "${OPENSEARCH_ENDPOINT?Set OPENSEARCH_ENDPOINT from terraform output opensearch_endpoint}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> Loading secrets from .env..."
set -a
source "$SCRIPT_DIR/.env"
set +a

echo "==> Config:"
echo "    RDS:         $RDS_ADDRESS"
echo "    Kafka:       $KAFKA_BOOTSTRAP_SERVERS"
echo "    OpenSearch:  $OPENSEARCH_ENDPOINT"

echo "==> Building and starting services..."
docker compose -f "$SCRIPT_DIR/docker-compose.prod.yml" up -d --build

echo ""
echo "==> Services:"
docker compose -f "$SCRIPT_DIR/docker-compose.prod.yml" ps
