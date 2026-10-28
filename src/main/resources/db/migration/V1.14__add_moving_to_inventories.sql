ALTER TABLE inventory.inventories
    ADD COLUMN moving BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN inventory.inventories.moving IS 'When true, categories are numbered moving boxes named "#n room"';
