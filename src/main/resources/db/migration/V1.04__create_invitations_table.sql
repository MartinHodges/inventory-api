-- Invitations table for managing inventory access invites
CREATE TABLE inventory.invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID NOT NULL REFERENCES inventory.inventories(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role inventory.member_role NOT NULL DEFAULT 'VIEWER',
    token VARCHAR(64) NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES inventory.users(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for token lookups (primary access pattern)
CREATE INDEX idx_invitations_token ON inventory.invitations(token);

-- Index for listing invitations by inventory
CREATE INDEX idx_invitations_inventory ON inventory.invitations(inventory_id);

-- Prevent duplicate pending invitations for same email/inventory
CREATE UNIQUE INDEX idx_invitations_inventory_email_pending
    ON inventory.invitations(inventory_id, email)
    WHERE accepted_at IS NULL;
