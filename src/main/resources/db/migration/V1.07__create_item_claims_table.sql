-- Item claims table for tracking user interest in items
CREATE TABLE inventory.item_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES inventory.items(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES inventory.users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'INTERESTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(item_id, user_id)
);

-- Index for finding all claims on an item
CREATE INDEX idx_item_claims_item ON inventory.item_claims(item_id);

-- Index for finding all claims by a user
CREATE INDEX idx_item_claims_user ON inventory.item_claims(user_id);

-- Index for finding assigned items
CREATE INDEX idx_item_claims_status ON inventory.item_claims(status);
