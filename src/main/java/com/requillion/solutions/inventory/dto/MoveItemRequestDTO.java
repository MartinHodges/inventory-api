package com.requillion.solutions.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveItemRequestDTO(
        @NotNull(message = "Category is required")
        UUID categoryId
) {}
