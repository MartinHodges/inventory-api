package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Item;

import java.time.Instant;
import java.util.UUID;

public record ItemResponseDTO(
        UUID id,
        UUID inventoryId,
        UUID categoryId,
        String categoryName,
        int referenceNumber,
        String description,
        boolean hasImage,
        int claimCount,
        boolean isAssigned,
        String assignedToName,
        boolean currentUserClaimed,
        Instant createdAt,
        Instant updatedAt
) {
    public static ItemResponseDTO toDTO(Item item, int claimCount, boolean isAssigned,
                                         String assignedToName, boolean currentUserClaimed) {
        return new ItemResponseDTO(
                item.getId(),
                item.getInventory().getId(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getReferenceNumber(),
                item.getDescription(),
                item.getImage() != null && item.getImage().length > 0,
                claimCount,
                isAssigned,
                assignedToName,
                currentUserClaimed,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
