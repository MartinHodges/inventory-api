package com.requillion.solutions.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemRequestDTO(
        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be less than 1000 characters")
        String description
) {}
