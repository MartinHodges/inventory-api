package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.MemberRole;

public record InvitationRequestDTO(
        String email,
        MemberRole role
) {
}
