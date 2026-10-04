CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(100) NOT NULL,
                       full_name VARCHAR(120) NOT NULL,
                       phone VARCHAR(30) UNIQUE,
                       role VARCHAR(20) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE addresses (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           label VARCHAR(40) NOT NULL,
                           recipient_name VARCHAR(120) NOT NULL,
                           phone VARCHAR(30) NOT NULL,
                           line1 VARCHAR(160) NOT NULL,
                           line2 VARCHAR(160),
                           city VARCHAR(80) NOT NULL,
                           state VARCHAR(80) NOT NULL,
                           postal_code VARCHAR(20) NOT NULL,
                           country_code CHAR(2) NOT NULL,
                           is_default BOOLEAN NOT NULL DEFAULT FALSE,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);