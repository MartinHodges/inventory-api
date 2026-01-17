package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.MemberRole;
import com.requillion.solutions.inventory.model.MemberStatus;

public record MemberUpdateDTO(
        MemberRole role,
        MemberStatus status
) {
}
