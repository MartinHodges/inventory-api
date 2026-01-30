-- Categories table
CREATE TABLE inventory.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID NOT NULL REFERENCES inventory.inventories(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_category_name_per_inventory UNIQUE (inventory_id, name)
);

-- Indexes
CREATE INDEX idx_categories_inventory ON inventory.categories(inventory_id);

COMMENT ON TABLE inventory.categories IS 'Categories within an inventory for organizing items';
COMMENT ON COLUMN inventory.categories.display_order IS 'Order in which categories appear in the UI';

-- Add category_id to items table
ALTER TABLE inventory.items ADD COLUMN category_id UUID REFERENCES inventory.categories(id) ON DELETE CASCADE;

-- Drop old unique constraint on (inventory_id, reference_number)
DROP INDEX inventory.idx_items_inventory_ref;

-- Create new unique constraint on (category_id, reference_number)
-- Note: category_id is nullable during migration, so this allows items without categories temporarily
CREATE UNIQUE INDEX idx_items_category_ref ON inventory.items(category_id, reference_number) WHERE category_id IS NOT NULL;

-- Index for querying items by category
CREATE INDEX idx_items_category ON inventory.items(category_id) WHERE category_id IS NOT NULL;

COMMENT ON COLUMN inventory.items.category_id IS 'Category this item belongs to';
