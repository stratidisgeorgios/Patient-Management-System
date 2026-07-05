CREATE TABLE charge_event (
    id UUID,
    patient_id VARCHAR(255),
    treatment_name VARCHAR(255),
    category VARCHAR(255),
    price VARCHAR(255),
    timestamp TIMESTAMP,
    PRIMARY KEY (id, timestamp)
);

CREATE TABLE patient_event (
    id UUID,
    patient_id VARCHAR(255),
    event_type VARCHAR(255),
    date_of_birth VARCHAR(255),
    gender VARCHAR(255),
    timestamp TIMESTAMP,
    PRIMARY KEY (id, timestamp)
);
