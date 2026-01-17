-- Inventories table
CREATE TABLE inventory.inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES inventory.users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_inventories_owner ON inventory.inventories(owner_id);

-- Unique constraint: user cannot have duplicate inventory names
CREATE UNIQUE INDEX idx_inventories_owner_name ON inventory.inventories(owner_id, name);

COMMENT ON TABLE inventory.inventories IS 'House inventories containing items';
COMMENT ON COLUMN inventory.inventories.owner_id IS 'User who created and owns this inventory';
