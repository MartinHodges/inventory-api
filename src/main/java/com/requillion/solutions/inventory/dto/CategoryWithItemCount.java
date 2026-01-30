package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.Category;

public record CategoryWithItemCount(
        Category category,
        int itemCount
) {}
