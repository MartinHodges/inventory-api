package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.InventoryMember;
import com.requillion.solutions.inventory.model.MemberRole;
import com.requillion.solutions.inventory.model.MemberStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemberResponseDTO(
        UUID id,
        UUID userId,
        String userName,
        String email,
        MemberRole role,
        MemberStatus status,
        Instant createdAt
) {
    public static MemberResponseDTO fromEntity(InventoryMember member) {
        return new MemberResponseDTO(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getFirstName() + " " + member.getUser().getLastName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }

    public static List<MemberResponseDTO> fromEntities(List<InventoryMember> members) {
        return members.stream()
                .map(MemberResponseDTO::fromEntity)
                .toList();
    }
}
