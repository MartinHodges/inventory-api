package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.MemberRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryResponseDTO(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        String ownerName,
        boolean isOwner,
        MemberRole userRole,
        int itemCount,
        boolean isFinished,
        Instant createdAt,
        Instant updatedAt
) {
    public static InventoryResponseDTO toDTO(Inventory inventory, boolean isOwner, MemberRole userRole, int itemCount, boolean isFinished) {
        return new InventoryResponseDTO(
                inventory.getId(),
                inventory.getName(),
                inventory.getDescription(),
                inventory.getOwner().getId(),
                inventory.getOwner().getFirstName() + " " + inventory.getOwner().getLastName(),
                isOwner,
                isOwner ? MemberRole.ADMIN : userRole,
                itemCount,
                isFinished,
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

    public static List<InventoryResponseDTO> toDTO(List<InventoryWithMeta> inventories) {
        return inventories.stream()
                .map(i -> toDTO(i.inventory(), i.isOwner(), i.userRole(), i.itemCount(), i.isFinished()))
                .toList();
    }
}
