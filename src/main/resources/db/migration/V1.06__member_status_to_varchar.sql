-- Fix invitation_member status column type: change from PostgreSQL enum to VARCHAR
-- JPA @Enumerated(EnumType.STRING) sends VARCHAR, not PostgreSQL enum type
ALTER TABLE inventory.inventory_members
ALTER COLUMN status TYPE VARCHAR(255);
