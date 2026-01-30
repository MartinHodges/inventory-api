package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.CategoryRecentItemCountDTO;
import com.requillion.solutions.inventory.dto.CategoryRequestDTO;
import com.requillion.solutions.inventory.dto.CategoryResponseDTO;
import com.requillion.solutions.inventory.dto.CategoryVisibilityDTO;
import com.requillion.solutions.inventory.dto.CategoryWithItemCount;
import com.requillion.solutions.inventory.model.Category;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.CategoryService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getCategories(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getCategories: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        List<CategoryWithItemCount> categories = categoryService.getCategories(context.getUser(), inventoryId);
        return ResponseEntity.ok(CategoryResponseDTO.toDTO(categories));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getCategory(
            @PathVariable UUID inventoryId,
            @PathVariable UUID categoryId) {
        LoggerUtil.debug(log, "getCategory: inventory=%s, category=%s", inventoryId, categoryId);
        RequestContext context = UserContext.getContext();
        Category category = categoryService.getCategory(context.getUser(), inventoryId, categoryId);
        return ResponseEntity.ok(CategoryResponseDTO.toDTO(category, 0));
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody CategoryRequestDTO dto) {
        LoggerUtil.debug(log, "createCategory: inventory=%s, name=%s", inventoryId, dto.name());
        RequestContext context = UserContext.getContext();
        Category category = categoryService.createCategory(context.getUser(), inventoryId, dto);
        return ResponseEntity.ok(CategoryResponseDTO.toDTO(category, 0));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable UUID inventoryId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequestDTO dto) {
        LoggerUtil.debug(log, "updateCategory: inventory=%s, category=%s", inventoryId, categoryId);
        RequestContext context = UserContext.getContext();
        Category category = categoryService.updateCategory(context.getUser(), inventoryId, categoryId, dto);
        return ResponseEntity.ok(CategoryResponseDTO.toDTO(category, 0));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable UUID inventoryId,
            @PathVariable UUID categoryId) {
        LoggerUtil.debug(log, "deleteCategory: inventory=%s, category=%s", inventoryId, categoryId);
        RequestContext context = UserContext.getContext();
        categoryService.deleteCategory(context.getUser(), inventoryId, categoryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryId}/visibility")
    public ResponseEntity<CategoryResponseDTO> setCategoryVisibility(
            @PathVariable UUID inventoryId,
            @PathVariable UUID categoryId,
            @RequestBody CategoryVisibilityDTO dto) {
        LoggerUtil.debug(log, "setCategoryVisibility: inventory=%s, category=%s, hidden=%s",
                inventoryId, categoryId, dto.hidden());
        RequestContext context = UserContext.getContext();
        Category category = categoryService.setCategoryVisibility(
                context.getUser(), inventoryId, categoryId, dto.hidden());
        return ResponseEntity.ok(CategoryResponseDTO.toDTO(category, 0));
    }

    @GetMapping("/recent-counts")
    public ResponseEntity<List<CategoryRecentItemCountDTO>> getRecentItemCounts(
            @PathVariable UUID inventoryId,
            @RequestParam(defaultValue = "7") int days) {
        LoggerUtil.debug(log, "getRecentItemCounts: inventory=%s, days=%d", inventoryId, days);
        RequestContext context = UserContext.getContext();
        List<CategoryRecentItemCountDTO> counts = categoryService.getRecentItemCounts(
                context.getUser(), inventoryId, days);
        return ResponseEntity.ok(counts);
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderCategories(
            @PathVariable UUID inventoryId,
            @RequestBody List<UUID> categoryIds) {
        LoggerUtil.debug(log, "reorderCategories: inventory=%s, count=%d", inventoryId, categoryIds.size());
        RequestContext context = UserContext.getContext();
        categoryService.reorderCategories(context.getUser(), inventoryId, categoryIds);
        return ResponseEntity.noContent().build();
    }
}
