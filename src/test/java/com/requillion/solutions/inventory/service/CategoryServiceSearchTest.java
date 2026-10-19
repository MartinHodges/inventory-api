package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.CategorySearchMatchDTO;
import com.requillion.solutions.inventory.model.Category;
import com.requillion.solutions.inventory.model.ClaimStatus;
import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.Item;
import com.requillion.solutions.inventory.model.ItemClaim;
import com.requillion.solutions.inventory.model.User;
import com.requillion.solutions.inventory.repository.CategoryRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.repository.ItemClaimRepository;
import com.requillion.solutions.inventory.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceSearchTest {

    @Mock CategoryRepository categoryRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemClaimRepository claimRepository;
    @Mock InventoryService inventoryService;

    @InjectMocks CategoryService categoryService;

    private final User user = new User();
    private final Inventory inventory = new Inventory();
    private final UUID inventoryId = UUID.randomUUID();
    private final List<Category> categories = new ArrayList<>();
    private final List<Object[]> items = new ArrayList<>();

    private Category kitchen;
    private Category bedroom;

    @BeforeEach
    void setUp() {
        user.setId(UUID.randomUUID());
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryService.canUserViewInventory(user, inventory)).thenReturn(true);
        when(categoryRepository.findByInventoryOrderByDisplayOrderAsc(inventory)).thenReturn(categories);
        when(itemRepository.findSearchableItems(inventory)).thenReturn(items);
        when(claimRepository.findByItemIdIn(any())).thenReturn(List.of());

        kitchen = category("Box 1", "Kitchen stuff", false);
        bedroom = category("Box 2", null, false);
        item(kitchen, 1, "Blue kettle", false);
        item(kitchen, 2, "Red kettle", false);
        item(bedroom, 1, "Blue duvet", false);
    }

    @Test
    void requiresAllKeywordsCaseInsensitive() {
        canEdit(true);
        assertThat(search("KETTLE")).containsExactly(match(kitchen, 2));
        assertThat(search("blue kettle")).containsExactly(match(kitchen, 1));
        assertThat(search("blue")).containsExactly(match(kitchen, 1), match(bedroom, 1));
        assertThat(search("green kettle")).isEmpty();
    }

    @Test
    void matchesCategoryNameAndDescription() {
        canEdit(true);
        assertThat(search("kitchen")).containsExactly(match(kitchen, 0));
        assertThat(search("box 2")).containsExactly(match(bedroom, 0));
    }

    @Test
    void blankQueryReturnsNothing() {
        canEdit(true);
        assertThat(search("   ")).isEmpty();
    }

    @Test
    void viewerDoesNotSeeHiddenCategories() {
        bedroom.setHidden(true);
        canEdit(false);
        assertThat(search("blue")).containsExactly(match(kitchen, 1));
    }

    @Test
    void viewerOnlySeesCollectedItemsAssignedToThem() {
        UUID collectedId = item(bedroom, 2, "Kettle descaler", true);
        canEdit(false);
        assertThat(search("kettle")).containsExactly(match(kitchen, 2));

        Item claimedItem = new Item();
        claimedItem.setId(collectedId);
        ItemClaim claim = new ItemClaim();
        claim.setItem(claimedItem);
        claim.setUser(user);
        claim.setStatus(ClaimStatus.ASSIGNED);
        when(claimRepository.findByItemIdIn(List.of(collectedId))).thenReturn(List.of(claim));
        assertThat(search("kettle")).containsExactly(match(kitchen, 2), match(bedroom, 1));
    }

    private List<CategorySearchMatchDTO> search(String q) {
        return categoryService.searchCategories(user, inventoryId, q);
    }

    private void canEdit(boolean canEdit) {
        when(inventoryService.canUserEditInventory(user, inventory)).thenReturn(canEdit);
    }

    private Category category(String name, String description, boolean hidden) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setDescription(description);
        category.setHidden(hidden);
        categories.add(category);
        return category;
    }

    private UUID item(Category category, int ref, String description, boolean collected) {
        UUID id = UUID.randomUUID();
        items.add(new Object[]{id, category.getId(), ref, description, collected});
        return id;
    }

    private static CategorySearchMatchDTO match(Category category, int count) {
        return new CategorySearchMatchDTO(category.getId(), count);
    }
}
