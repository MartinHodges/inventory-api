package com.requillion.solutions.inventory.dto;

import java.util.UUID;

public record CategoryRecentItemCountDTO(
        UUID categoryId,
        int newCount,
        int updatedCount
) {
}
