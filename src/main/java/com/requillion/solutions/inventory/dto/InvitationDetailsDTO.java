package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Invitation;
import com.requillion.solutions.inventory.model.MemberRole;

import java.time.Instant;
import java.util.UUID;

public record InvitationDetailsDTO(
        UUID id,
        String inventoryName,
        String invitedByName,
        MemberRole role,
        Instant expiresAt,
        boolean isExpired,
        boolean isAccepted
) {
    public static InvitationDetailsDTO fromEntity(Invitation invitation) {
        return new InvitationDetailsDTO(
                invitation.getId(),
                invitation.getInventory().getName(),
                invitation.getInvitedBy().getFirstName() + " " + invitation.getInvitedBy().getLastName(),
                invitation.getRole(),
                invitation.getExpiresAt(),
                invitation.isExpired(),
                invitation.isAccepted()
        );
    }
}
