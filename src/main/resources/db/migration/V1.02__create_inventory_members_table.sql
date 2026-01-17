-- Inventory member roles
CREATE TYPE inventory.member_role AS ENUM ('ADMIN', 'CLAIMANT', 'VIEWER');
CREATE TYPE inventory.member_status AS ENUM ('PENDING', 'ACTIVE');

-- Inventory members table
CREATE TABLE inventory.inventory_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID NOT NULL REFERENCES inventory.inventories(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES inventory.users(id),
    role inventory.member_role NOT NULL DEFAULT 'VIEWER',
    status inventory.member_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_inventory_members_inventory ON inventory.inventory_members(inventory_id);
CREATE INDEX idx_inventory_members_user ON inventory.inventory_members(user_id);
CREATE UNIQUE INDEX idx_inventory_members_unique ON inventory.inventory_members(inventory_id, user_id);

COMMENT ON TABLE inventory.inventory_members IS 'Users who have access to inventories';
COMMENT ON COLUMN inventory.inventory_members.role IS 'ADMIN can manage, CLAIMANT can claim items, VIEWER can only view';
COMMENT ON COLUMN inventory.inventory_members.status IS 'PENDING until admin approves, then ACTIVE';
