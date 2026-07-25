CREATE TABLE patient (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    gender VARCHAR(10),
    address VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    registered_date DATE NOT NULL,
    custom_fields JSONB
);

CREATE TABLE import_job (
    id UUID PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_rows INT,
    imported_rows INT DEFAULT 0,
    failed_rows INT DEFAULT 0,
    errors JSONB,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);