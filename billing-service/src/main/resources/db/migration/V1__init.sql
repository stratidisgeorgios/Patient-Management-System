CREATE TABLE billing_account (
    id UUID PRIMARY KEY,
    patient_id VARCHAR(255),
    patient_name VARCHAR(255),
    patient_email VARCHAR(255),
    balance NUMERIC
);

CREATE TABLE charge (
    id UUID PRIMARY KEY,
    billing_account_id UUID,
    treatment_id VARCHAR(255),
    treatment_name VARCHAR(255),
    treatment_category VARCHAR(255),
    price NUMERIC,
    timestamp TIMESTAMP
);

CREATE INDEX idx_charge_billing_account_id ON charge(billing_account_id);
