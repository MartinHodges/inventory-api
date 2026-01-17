-- Create schema
CREATE SCHEMA IF NOT EXISTS inventory;

-- Users table
CREATE TABLE inventory.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for keycloak lookup
CREATE INDEX idx_users_keycloak_id ON inventory.users(keycloak_id);

-- Index for email lookup
CREATE INDEX idx_users_email ON inventory.users(email);

COMMENT ON TABLE inventory.users IS 'Users who can access inventories';
COMMENT ON COLUMN inventory.users.keycloak_id IS 'Keycloak subject ID for SSO';
