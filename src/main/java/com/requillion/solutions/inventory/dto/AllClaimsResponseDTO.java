package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.MemberRole;

import java.util.List;
import java.util.UUID;

public record AllClaimsResponseDTO(
        UUID userId,
        UUID memberId,
        String userName,
        MemberRole role,
        boolean isFinished,
        List<ClaimedItemDTO> claims
) {
}
