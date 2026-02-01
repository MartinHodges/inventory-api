package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.ClaimStatus;
import com.requillion.solutions.inventory.model.Item;
import com.requillion.solutions.inventory.model.ItemClaim;

import java.util.Base64;
import java.util.UUID;

public record ClaimedItemDTO(
        UUID itemId,
        int referenceNumber,
        String categoryName,
        String description,
        String thumbnailBase64,
        ClaimStatus claimStatus,
        boolean isCollected,
        int claimCount
) {
    public static ClaimedItemDTO fromClaim(ItemClaim claim, int claimCount) {
        Item item = claim.getItem();
        String thumbnail = null;
        if (item.getThumbnail() != null && item.getThumbnail().length > 0) {
            thumbnail = Base64.getEncoder().encodeToString(item.getThumbnail());
        }
        return new ClaimedItemDTO(
                item.getId(),
                item.getReferenceNumber(),
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getDescription(),
                thumbnail,
                claim.getStatus(),
                item.getIsCollected(),
                claimCount
        );
    }
}
