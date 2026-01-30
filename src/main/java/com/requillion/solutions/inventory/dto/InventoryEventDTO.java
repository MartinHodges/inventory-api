package com.requillion.solutions.inventory.dto;

import java.util.UUID;

public record InventoryEventDTO(
        String type,
        UUID inventoryId,
        UUID itemId,
        UUID claimId,
        long timestamp
) {
    public static InventoryEventDTO itemCreated(UUID inventoryId, UUID itemId) {
        return new InventoryEventDTO("ITEM_CREATED", inventoryId, itemId, null, System.currentTimeMillis());
    }

    public static InventoryEventDTO itemUpdated(UUID inventoryId, UUID itemId) {
        return new InventoryEventDTO("ITEM_UPDATED", inventoryId, itemId, null, System.currentTimeMillis());
    }

    public static InventoryEventDTO itemDeleted(UUID inventoryId, UUID itemId) {
        return new InventoryEventDTO("ITEM_DELETED", inventoryId, itemId, null, System.currentTimeMillis());
    }

    public static InventoryEventDTO itemUndeleted(UUID inventoryId, UUID itemId) {
        return new InventoryEventDTO("ITEM_UNDELETED", inventoryId, itemId, null, System.currentTimeMillis());
    }

    public static InventoryEventDTO claimCreated(UUID inventoryId, UUID itemId, UUID claimId) {
        return new InventoryEventDTO("CLAIM_CREATED", inventoryId, itemId, claimId, System.currentTimeMillis());
    }

    public static InventoryEventDTO claimDeleted(UUID inventoryId, UUID itemId, UUID claimId) {
        return new InventoryEventDTO("CLAIM_DELETED", inventoryId, itemId, claimId, System.currentTimeMillis());
    }

    public static InventoryEventDTO itemAssigned(UUID inventoryId, UUID itemId, UUID claimId) {
        return new InventoryEventDTO("ITEM_ASSIGNED", inventoryId, itemId, claimId, System.currentTimeMillis());
    }

    public static InventoryEventDTO itemUnassigned(UUID inventoryId, UUID itemId) {
        return new InventoryEventDTO("ITEM_UNASSIGNED", inventoryId, itemId, null, System.currentTimeMillis());
    }
}
