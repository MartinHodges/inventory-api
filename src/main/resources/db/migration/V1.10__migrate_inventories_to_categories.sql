-- Data Migration: Convert existing inventories to categories
-- This migration converts 11 existing inventories into categories under 2 new parent inventories

-- Step 1: Get the owner_id from existing inventories (assumes single owner)
DO $$
DECLARE
    v_owner_id UUID;
    v_dads_estate_id UUID;
    v_playground_id UUID;
BEGIN
    -- Get owner from existing inventories
    SELECT DISTINCT owner_id INTO v_owner_id FROM inventory.inventories LIMIT 1;

    IF v_owner_id IS NULL THEN
        RAISE EXCEPTION 'No existing inventories found';
    END IF;

    -- Step 2: Create new parent inventories
    INSERT INTO inventory.inventories (name, description, owner_id)
    VALUES ('Dad''s Estate', 'Estate items from Dad', v_owner_id)
    RETURNING id INTO v_dads_estate_id;

    INSERT INTO inventory.inventories (name, description, owner_id)
    VALUES ('Playground', 'Test and miscellaneous items', v_owner_id)
    RETURNING id INTO v_playground_id;

    -- Step 3a: Create categories from "Dad" inventories
    INSERT INTO inventory.categories (inventory_id, name, description, display_order)
    SELECT
        v_dads_estate_id,
        old.name,
        old.description,
        (ROW_NUMBER() OVER (ORDER BY old.name) - 1)::INTEGER
    FROM inventory.inventories old
    WHERE old.name ILIKE '%Dad%'
      AND old.id NOT IN (v_dads_estate_id, v_playground_id);

    -- Step 3b: Create categories from non-"Dad" inventories
    INSERT INTO inventory.categories (inventory_id, name, description, display_order)
    SELECT
        v_playground_id,
        old.name,
        old.description,
        (ROW_NUMBER() OVER (ORDER BY old.name) - 1)::INTEGER
    FROM inventory.inventories old
    WHERE old.name NOT ILIKE '%Dad%'
      AND old.id NOT IN (v_dads_estate_id, v_playground_id);

    -- Step 4: Update items to point to new categories
    UPDATE inventory.items i
    SET category_id = c.id,
        inventory_id = c.inventory_id
    FROM inventory.categories c
    JOIN inventory.inventories old ON old.name = c.name
    WHERE i.inventory_id = old.id
      AND old.id NOT IN (v_dads_estate_id, v_playground_id);

    -- Step 5: Merge memberships - insert unique combinations with lowest role
    -- Role priority: VIEWER (lowest) < CLAIMANT < ADMIN (highest)
    -- We want to keep the LOWEST role when merging

    -- For Dad's Estate: collect members from Dad inventories
    INSERT INTO inventory.inventory_members (inventory_id, user_id, role, status)
    SELECT DISTINCT ON (user_id)
        v_dads_estate_id,
        im.user_id,
        -- Select the lowest role (VIEWER < CLAIMANT < ADMIN)
        (SELECT role FROM inventory.inventory_members im2
         JOIN inventory.inventories inv2 ON im2.inventory_id = inv2.id
         WHERE inv2.name ILIKE '%Dad%'
           AND inv2.id NOT IN (v_dads_estate_id, v_playground_id)
           AND im2.user_id = im.user_id
         ORDER BY CASE role
             WHEN 'VIEWER' THEN 1
             WHEN 'CLAIMANT' THEN 2
             WHEN 'ADMIN' THEN 3
         END ASC
         LIMIT 1),
        'ACTIVE'
    FROM inventory.inventory_members im
    JOIN inventory.inventories inv ON im.inventory_id = inv.id
    WHERE inv.name ILIKE '%Dad%'
      AND inv.id NOT IN (v_dads_estate_id, v_playground_id)
    ON CONFLICT (inventory_id, user_id) DO NOTHING;

    -- For Playground: collect members from non-Dad inventories
    INSERT INTO inventory.inventory_members (inventory_id, user_id, role, status)
    SELECT DISTINCT ON (user_id)
        v_playground_id,
        im.user_id,
        (SELECT role FROM inventory.inventory_members im2
         JOIN inventory.inventories inv2 ON im2.inventory_id = inv2.id
         WHERE inv2.name NOT ILIKE '%Dad%'
           AND inv2.id NOT IN (v_dads_estate_id, v_playground_id)
           AND im2.user_id = im.user_id
         ORDER BY CASE role
             WHEN 'VIEWER' THEN 1
             WHEN 'CLAIMANT' THEN 2
             WHEN 'ADMIN' THEN 3
         END ASC
         LIMIT 1),
        'ACTIVE'
    FROM inventory.inventory_members im
    JOIN inventory.inventories inv ON im.inventory_id = inv.id
    WHERE inv.name NOT ILIKE '%Dad%'
      AND inv.id NOT IN (v_dads_estate_id, v_playground_id)
    ON CONFLICT (inventory_id, user_id) DO NOTHING;

    -- Step 6: Delete pending invitations for old inventories
    DELETE FROM inventory.invitations
    WHERE inventory_id NOT IN (v_dads_estate_id, v_playground_id);

    -- Step 7: Delete old inventories (items already moved, FK cascades handle cleanup)
    DELETE FROM inventory.inventories
    WHERE id NOT IN (v_dads_estate_id, v_playground_id);

    RAISE NOTICE 'Migration complete. Created inventories: Dad''s Estate (%), Playground (%)',
        v_dads_estate_id, v_playground_id;
END $$;
