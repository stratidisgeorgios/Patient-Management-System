CREATE TABLE category (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE treatment (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    category_id UUID REFERENCES category(id),
    price NUMERIC
);
