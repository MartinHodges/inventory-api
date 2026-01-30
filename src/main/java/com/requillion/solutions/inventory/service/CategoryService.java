package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.CategoryRequestDTO;
import com.requillion.solutions.inventory.dto.CategoryWithItemCount;
import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.Category;
import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.User;
import com.requillion.solutions.inventory.repository.CategoryRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.repository.ItemRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ItemRepository itemRepository;
    private final InventoryService inventoryService;

    public List<CategoryWithItemCount> getCategories(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = getInventoryWithAccess(user, inventoryId);
        boolean canEdit = inventoryService.canUserEditInventory(user, inventory);

        List<Category> categories = categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory);

        return categories.stream()
                .filter(cat -> canEdit || !cat.getHidden())
                .map(cat -> new CategoryWithItemCount(cat,
                        (int) itemRepository.countByCategoryAndIsDeletedFalse(cat)))
                .toList();
    }

    public Category getCategory(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID categoryId) {
        Inventory inventory = getInventoryWithAccess(user, inventoryId);

        Category category = categoryRepository.findByInventoryAndId(inventory, categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category not found",
                        "Category: %s, Inventory: %s", categoryId, inventoryId));

        if (category.getHidden() && !inventoryService.canUserEditInventory(user, inventory)) {
            throw new NotFoundException(
                    "Category not found",
                    "Category: %s is hidden, User: %s", categoryId, user.getId());
        }

        return category;
    }

    public Category createCategory(@NonNull User user, @NonNull UUID inventoryId, @NonNull CategoryRequestDTO dto) {
        Inventory inventory = getInventoryWithEditAccess(user, inventoryId);

        // Check for duplicate name
        if (categoryRepository.findByInventoryAndName(inventory, dto.name()).isPresent()) {
            throw new BadInputException(
                    "A category named '" + dto.name() + "' already exists in this inventory",
                    "Category name: %s, Inventory: %s", dto.name(), inventoryId);
        }

        // Get next display order
        int nextOrder = categoryRepository.findMaxDisplayOrder(inventory) + 1;

        Category category = new Category();
        category.setInventory(inventory);
        category.setName(dto.name());
        category.setDescription(dto.description());
        category.setDisplayOrder(nextOrder);
        category.setHidden(true);

        category = categoryRepository.save(category);
        LoggerUtil.info(log, "Created category %s in inventory %s", category.getId(), inventoryId);

        return category;
    }

    public Category updateCategory(@NonNull User user, @NonNull UUID inventoryId,
                                   @NonNull UUID categoryId, @NonNull CategoryRequestDTO dto) {
        Inventory inventory = getInventoryWithEditAccess(user, inventoryId);

        Category category = categoryRepository.findByInventoryAndId(inventory, categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category not found",
                        "Category: %s, Inventory: %s", categoryId, inventoryId));

        // Check for duplicate name (excluding current category)
        categoryRepository.findByInventoryAndName(inventory, dto.name())
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new BadInputException(
                            "A category named '" + dto.name() + "' already exists in this inventory",
                            "Category name: %s, Inventory: %s", dto.name(), inventoryId);
                });

        category.setName(dto.name());
        category.setDescription(dto.description());

        category = categoryRepository.save(category);
        LoggerUtil.info(log, "Updated category %s", category.getId());

        return category;
    }

    public void deleteCategory(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID categoryId) {
        Inventory inventory = getInventoryWithEditAccess(user, inventoryId);

        Category category = categoryRepository.findByInventoryAndId(inventory, categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category not found",
                        "Category: %s, Inventory: %s", categoryId, inventoryId));

        // Check if category has items
        long itemCount = itemRepository.countByCategoryAndIsDeletedFalse(category);
        if (itemCount > 0) {
            throw new BadInputException(
                    "Cannot delete a category that contains items. Remove or move all items first.",
                    "Category: %s has %d items", categoryId, itemCount);
        }

        categoryRepository.delete(category);
        LoggerUtil.info(log, "Deleted category %s", categoryId);
    }

    public void reorderCategories(@NonNull User user, @NonNull UUID inventoryId, @NonNull List<UUID> categoryIds) {
        Inventory inventory = getInventoryWithEditAccess(user, inventoryId);

        List<Category> categories = categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory);

        // Validate all category IDs belong to this inventory
        if (categoryIds.size() != categories.size()) {
            throw new BadInputException(
                    "Category list must contain all categories in the inventory",
                    "Expected %d categories, got %d", categories.size(), categoryIds.size());
        }

        for (int i = 0; i < categoryIds.size(); i++) {
            UUID categoryId = categoryIds.get(i);
            Category category = categories.stream()
                    .filter(c -> c.getId().equals(categoryId))
                    .findFirst()
                    .orElseThrow(() -> new BadInputException(
                            "Category not found in this inventory",
                            "Category: %s, Inventory: %s", categoryId, inventoryId));
            category.setDisplayOrder(i);
            categoryRepository.save(category);
        }

        LoggerUtil.info(log, "Reordered categories in inventory %s", inventoryId);
    }

    public Category setCategoryVisibility(@NonNull User user, @NonNull UUID inventoryId,
                                            @NonNull UUID categoryId, boolean hidden) {
        Inventory inventory = getInventoryWithEditAccess(user, inventoryId);

        Category category = categoryRepository.findByInventoryAndId(inventory, categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category not found",
                        "Category: %s, Inventory: %s", categoryId, inventoryId));

        category.setHidden(hidden);
        category = categoryRepository.save(category);
        LoggerUtil.info(log, "Set category %s hidden=%s", category.getId(), hidden);

        return category;
    }

    private Inventory getInventoryWithAccess(User user, UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserViewInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have access to this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        return inventory;
    }

    private Inventory getInventoryWithEditAccess(User user, UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserEditInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to edit this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        return inventory;
    }
}
