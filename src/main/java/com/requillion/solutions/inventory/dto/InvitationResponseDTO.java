package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Invitation;
import com.requillion.solutions.inventory.model.MemberRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvitationResponseDTO(
        UUID id,
        String email,
        MemberRole role,
        String invitedByName,
        Instant expiresAt,
        Instant createdAt,
        boolean isExpired,
        boolean isAccepted
) {
    public static InvitationResponseDTO fromEntity(Invitation invitation) {
        return new InvitationResponseDTO(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getInvitedBy().getFirstName() + " " + invitation.getInvitedBy().getLastName(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                invitation.isExpired(),
                invitation.isAccepted()
        );
    }

    public static List<InvitationResponseDTO> fromEntities(List<Invitation> invitations) {
        return invitations.stream()
                .map(InvitationResponseDTO::fromEntity)
                .toList();
    }
}
