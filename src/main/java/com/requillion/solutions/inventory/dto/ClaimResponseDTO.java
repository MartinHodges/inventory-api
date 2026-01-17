package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.ClaimStatus;
import com.requillion.solutions.inventory.model.ItemClaim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimResponseDTO(
        UUID id,
        UUID itemId,
        UUID userId,
        String userName,
        ClaimStatus status,
        Instant createdAt
) {
    public static ClaimResponseDTO fromEntity(ItemClaim claim) {
        return new ClaimResponseDTO(
                claim.getId(),
                claim.getItem().getId(),
                claim.getUser().getId(),
                claim.getUser().getFirstName() + " " + claim.getUser().getLastName(),
                claim.getStatus(),
                claim.getCreatedAt()
        );
    }

    public static List<ClaimResponseDTO> fromEntities(List<ItemClaim> claims) {
        return claims.stream()
                .map(ClaimResponseDTO::fromEntity)
                .toList();
    }
}
