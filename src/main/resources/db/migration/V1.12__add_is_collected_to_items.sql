ALTER TABLE inventory.items
    ADD COLUMN is_collected BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_items_category_not_deleted_not_collected
    ON inventory.items (category_id)
    WHERE is_deleted = FALSE AND is_collected = FALSE;
