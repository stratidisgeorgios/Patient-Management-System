CREATE TABLE category (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    organization_id VARCHAR(255),
    UNIQUE (name, organization_id)
);

CREATE TABLE treatment (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id UUID REFERENCES category(id),
    price NUMERIC,
    organization_id VARCHAR(255),
    UNIQUE (name, organization_id)
);
