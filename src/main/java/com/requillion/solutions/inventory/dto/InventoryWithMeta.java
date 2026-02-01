package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.MemberRole;

public record InventoryWithMeta(
        Inventory inventory,
        boolean isOwner,
        MemberRole userRole,
        int itemCount,
        boolean isFinished
) {}
