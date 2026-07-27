#!/bin/bash
# Run this once on the EC2 instance after RDS is up.
# Usage: ./init_databases.sh <rds-host> <db-username> <db-password>

HOST=$1
USER=$2
export PGPASSWORD=$3

psql -h "$HOST" -U "$USER" -d postgres -c "CREATE DATABASE billing_db;"
psql -h "$HOST" -U "$USER" -d postgres -c "CREATE DATABASE analytics_db;"
psql -h "$HOST" -U "$USER" -d postgres -c "CREATE DATABASE treatment_db;"
psql -h "$HOST" -U "$USER" -d postgres -c "CREATE DATABASE organization_db;"

echo "All databases created."
