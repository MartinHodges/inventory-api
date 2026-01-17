package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Item;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record ItemWithThumbnailDTO(
        UUID id,
        UUID inventoryId,
        int referenceNumber,
        String description,
        String thumbnailBase64,
        int claimCount,
        boolean isAssigned,
        String assignedToName,
        Instant createdAt,
        Instant updatedAt
) {
    public static ItemWithThumbnailDTO toDTO(Item item, int claimCount, boolean isAssigned, String assignedToName) {
        String thumbnail = null;
        if (item.getThumbnail() != null && item.getThumbnail().length > 0) {
            thumbnail = Base64.getEncoder().encodeToString(item.getThumbnail());
        }
        return new ItemWithThumbnailDTO(
                item.getId(),
                item.getInventory().getId(),
                item.getReferenceNumber(),
                item.getDescription(),
                thumbnail,
                claimCount,
                isAssigned,
                assignedToName,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
