-- Fix invitation role column type: change from PostgreSQL enum to VARCHAR
-- JPA @Enumerated(EnumType.STRING) sends VARCHAR, not PostgreSQL enum type
ALTER TABLE inventory.invitations
ALTER COLUMN role TYPE VARCHAR(50) USING role::text;
