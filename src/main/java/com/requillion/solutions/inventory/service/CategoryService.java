package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.CategoryRecentItemCountDTO;
import com.requillion.solutions.inventory.dto.CategoryRequestDTO;
import com.requillion.solutions.inventory.dto.CategorySearchMatchDTO;
import com.requillion.solutions.inventory.dto.CategoryWithItemCount;
import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.Category;
import com.requillion.solutions.inventory.model.ClaimStatus;
import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.User;
import com.requillion.solutions.inventory.repository.CategoryRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.repository.ItemClaimRepository;
import com.requillion.solutions.inventory.repository.ItemRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import com.requillion.solutions.inventory.util.MovingBoxNames;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ItemRepository itemRepository;
    private final ItemClaimRepository claimRepository;
    private final InventoryService inventoryService;

    public List<CategoryWithItemCount> getCategories(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = getInventoryWithAccess(user, inventoryId);
        boolean canEdit = inventoryService.canUserEditInventory(user, inventory);

        List<Category> categories = categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory);
        if (Boolean.TRUE.equals(inventory.getMoving())) {
            categories = categories.stream()
                    .sorted(MovingBoxNames.comparing(Category::getName))
                    .toList();
        }

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

        // In a moving inventory the name given is a room, which becomes the next box "#n room"
        String name = dto.name();
        if (Boolean.TRUE.equals(inventory.getMoving())) {
            List<String> existingNames = categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory).stream()
                    .map(Category::getName)
                    .toList();
            name = MovingBoxNames.nextName(name, existingNames);
        }

        // Check for duplicate name
        if (categoryRepository.findByInventoryAndName(inventory, name).isPresent()) {
            throw new BadInputException(
                    "A category named '" + name + "' already exists in this inventory",
                    "Category name: %s, Inventory: %s", name, inventoryId);
        }

        // Get next display order
        int nextOrder = categoryRepository.findMaxDisplayOrder(inventory) + 1;

        Category category = new Category();
        category.setInventory(inventory);
        category.setName(name);
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

    public List<CategoryRecentItemCountDTO> getRecentItemCounts(@NonNull User user, @NonNull UUID inventoryId, int days) {
        Inventory inventory = getInventoryWithAccess(user, inventoryId);
        boolean canEdit = inventoryService.canUserEditInventory(user, inventory);

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> rows = itemRepository.countRecentItemsByCategory(inventory, since);

        // If user can't edit, filter out hidden categories
        Set<UUID> visibleCategoryIds;
        if (!canEdit) {
            visibleCategoryIds = categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory).stream()
                    .filter(cat -> !cat.getHidden())
                    .map(Category::getId)
                    .collect(Collectors.toSet());
        } else {
            visibleCategoryIds = null;
        }

        return rows.stream()
                .filter(row -> visibleCategoryIds == null || visibleCategoryIds.contains((UUID) row[0]))
                .map(row -> new CategoryRecentItemCountDTO(
                        (UUID) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()))
                .toList();
    }

    public List<CategorySearchMatchDTO> searchCategories(@NonNull User user, @NonNull UUID inventoryId, String query) {
        Inventory inventory = getInventoryWithAccess(user, inventoryId);
        boolean canEdit = inventoryService.canUserEditInventory(user, inventory);

        List<String> keywords = query == null ? List.of() : Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(k -> !k.isBlank())
                .toList();
        if (keywords.isEmpty()) {
            return List.of();
        }

        List<Object[]> rows = itemRepository.findSearchableItems(inventory);

        // Users who can't edit only see collected items that are assigned to them
        Set<UUID> assignedCollectedIds = Set.of();
        if (!canEdit) {
            List<UUID> collectedIds = rows.stream()
                    .filter(row -> Boolean.TRUE.equals(row[4]))
                    .map(row -> (UUID) row[0])
                    .toList();
            if (!collectedIds.isEmpty()) {
                assignedCollectedIds = claimRepository.findByItemIdIn(collectedIds).stream()
                        .filter(c -> c.getStatus() == ClaimStatus.ASSIGNED && c.getUser().equals(user))
                        .map(c -> c.getItem().getId())
                        .collect(Collectors.toSet());
            }
        }

        Map<UUID, Integer> matchCounts = new HashMap<>();
        for (Object[] row : rows) {
            boolean visible = canEdit || !Boolean.TRUE.equals(row[4]) || assignedCollectedIds.contains((UUID) row[0]);
            if (visible && matchesAll(row[2] + " " + row[3], keywords)) {
                matchCounts.merge((UUID) row[1], 1, Integer::sum);
            }
        }

        return categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory).stream()
                .filter(cat -> canEdit || !cat.getHidden())
                .filter(cat -> matchCounts.containsKey(cat.getId())
                        || matchesAll(cat.getName() + " " + Objects.toString(cat.getDescription(), ""), keywords))
                .map(cat -> new CategorySearchMatchDTO(cat.getId(), matchCounts.getOrDefault(cat.getId(), 0)))
                .toList();
    }

    private static boolean matchesAll(String text, List<String> keywords) {
        String lower = text.toLowerCase();
        return keywords.stream().allMatch(lower::contains);
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
