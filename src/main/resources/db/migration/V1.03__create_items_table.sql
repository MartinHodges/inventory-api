-- Items table
CREATE TABLE inventory.items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID NOT NULL REFERENCES inventory.inventories(id) ON DELETE CASCADE,
    reference_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    image BYTEA,
    thumbnail BYTEA,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_items_inventory ON inventory.items(inventory_id);
CREATE INDEX idx_items_inventory_not_deleted ON inventory.items(inventory_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_items_inventory_ref ON inventory.items(inventory_id, reference_number);

COMMENT ON TABLE inventory.items IS 'Items within an inventory';
COMMENT ON COLUMN inventory.items.reference_number IS 'Sequential number within the inventory';
COMMENT ON COLUMN inventory.items.image IS 'Compressed image stored as binary';
COMMENT ON COLUMN inventory.items.thumbnail IS 'Small thumbnail for list views';
COMMENT ON COLUMN inventory.items.is_deleted IS 'Soft delete flag';
