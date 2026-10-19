package com.requillion.solutions.inventory.dto;

import java.util.UUID;

public record CategorySearchMatchDTO(
        UUID categoryId,
        int matchCount
) {
}
