package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Category;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        UUID inventoryId,
        String name,
        String description,
        int displayOrder,
        int itemCount,
        boolean hidden,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponseDTO toDTO(Category category, int itemCount) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getInventory().getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                itemCount,
                category.getHidden(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public static List<CategoryResponseDTO> toDTO(List<CategoryWithItemCount> categories) {
        return categories.stream()
                .map(c -> toDTO(c.category(), c.itemCount()))
                .toList();
    }
}
