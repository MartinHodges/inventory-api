package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.ItemRequestDTO;
import com.requillion.solutions.inventory.dto.ItemResponseDTO;
import com.requillion.solutions.inventory.dto.ItemWithThumbnailDTO;
import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.*;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.repository.ItemClaimRepository;
import com.requillion.solutions.inventory.repository.ItemRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final ImageService imageService;
    private final ItemClaimRepository claimRepository;

    public List<Item> getItems(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserViewInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have access to this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        List<Item> items = itemRepository.findByInventoryAndIsDeletedFalseOrderByReferenceNumberAsc(inventory);
        LoggerUtil.info(log, "Retrieved %d items from inventory %s", items.size(), inventoryId);
        return items;
    }

    public Item getItem(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserViewInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have access to this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        return itemRepository.findByInventoryAndIdAndIsDeletedFalse(inventory, itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Item not found",
                        "Item: %s, Inventory: %s", itemId, inventoryId));
    }

    public Item createItem(@NonNull User user, @NonNull UUID inventoryId,
                           @NonNull ItemRequestDTO dto, byte[] imageData) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserEditInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to add items to this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        // Generate reference number
        Integer maxRef = itemRepository.findMaxReferenceNumber(inventory);
        int newRefNumber = (maxRef != null ? maxRef : 0) + 1;

        Item item = new Item();
        item.setInventory(inventory);
        item.setReferenceNumber(newRefNumber);
        item.setDescription(dto.description());

        // Process image if provided
        if (imageData != null && imageData.length > 0) {
            try {
                item.setImage(imageService.compressImage(imageData, newRefNumber));
                item.setThumbnail(imageService.createThumbnail(imageData));
                LoggerUtil.debug(log, "Processed image for item: original=%d, compressed=%d, thumbnail=%d",
                        imageData.length, item.getImage().length, item.getThumbnail().length);
            } catch (IOException e) {
                throw new BadInputException(
                        "Failed to process image",
                        "Error: %s", e.getMessage());
            }
        }

        item = itemRepository.save(item);
        LoggerUtil.info(log, "Created item %s (#%d) in inventory %s",
                item.getId(), item.getReferenceNumber(), inventoryId);

        return item;
    }

    public Item updateItem(@NonNull User user, @NonNull UUID inventoryId,
                           @NonNull UUID itemId, @NonNull ItemRequestDTO dto) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserEditInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to edit items in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        Item item = itemRepository.findByInventoryAndIdAndIsDeletedFalse(inventory, itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Item not found",
                        "Item: %s, Inventory: %s", itemId, inventoryId));

        item.setDescription(dto.description());
        item = itemRepository.save(item);

        LoggerUtil.info(log, "Updated item %s", item.getId());
        return item;
    }

    public Item updateItemImage(@NonNull User user, @NonNull UUID inventoryId,
                                @NonNull UUID itemId, @NonNull byte[] imageData) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserEditInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to edit items in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        Item item = itemRepository.findByInventoryAndIdAndIsDeletedFalse(inventory, itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Item not found",
                        "Item: %s, Inventory: %s", itemId, inventoryId));

        try {
            item.setImage(imageService.compressImage(imageData, item.getReferenceNumber()));
            item.setThumbnail(imageService.createThumbnail(imageData));
        } catch (IOException e) {
            throw new BadInputException(
                    "Failed to process image",
                    "Error: %s", e.getMessage());
        }

        item = itemRepository.save(item);
        LoggerUtil.info(log, "Updated image for item %s", item.getId());
        return item;
    }

    public void deleteItem(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!inventoryService.canUserEditInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to delete items from this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        Item item = itemRepository.findByInventoryAndIdAndIsDeletedFalse(inventory, itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Item not found",
                        "Item: %s, Inventory: %s", itemId, inventoryId));

        // Soft delete
        item.setIsDeleted(true);
        itemRepository.save(item);

        LoggerUtil.info(log, "Soft-deleted item %s from inventory %s", itemId, inventoryId);
    }

    public byte[] getItemImage(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItem(user, inventoryId, itemId);

        if (item.getImage() == null || item.getImage().length == 0) {
            throw new NotFoundException(
                    "Item has no image",
                    "Item: %s", itemId);
        }

        return item.getImage();
    }

    public byte[] getItemThumbnail(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItem(user, inventoryId, itemId);

        if (item.getThumbnail() == null || item.getThumbnail().length == 0) {
            throw new NotFoundException(
                    "Item has no thumbnail",
                    "Item: %s", itemId);
        }

        return item.getThumbnail();
    }

    // DTO methods with claim information

    public List<ItemWithThumbnailDTO> getItemsWithThumbnails(@NonNull User user, @NonNull UUID inventoryId) {
        List<Item> items = getItems(user, inventoryId);

        if (items.isEmpty()) {
            return List.of();
        }

        // Get all claims for these items in one query
        List<UUID> itemIds = items.stream().map(Item::getId).toList();
        List<ItemClaim> allClaims = claimRepository.findByItemIdIn(itemIds);

        // Group claims by item ID
        Map<UUID, List<ItemClaim>> claimsByItem = allClaims.stream()
                .collect(Collectors.groupingBy(c -> c.getItem().getId()));

        return items.stream().map(item -> {
            List<ItemClaim> itemClaims = claimsByItem.getOrDefault(item.getId(), List.of());
            int claimCount = itemClaims.size();
            ItemClaim assigned = itemClaims.stream()
                    .filter(c -> c.getStatus() == ClaimStatus.ASSIGNED)
                    .findFirst()
                    .orElse(null);
            boolean isAssigned = assigned != null;
            String assignedToName = isAssigned
                    ? assigned.getUser().getFirstName() + " " + assigned.getUser().getLastName()
                    : null;

            return ItemWithThumbnailDTO.toDTO(item, claimCount, isAssigned, assignedToName);
        }).toList();
    }

    public ItemResponseDTO getItemDTO(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItem(user, inventoryId, itemId);
        return buildItemResponseDTO(item, user);
    }

    public ItemResponseDTO createItemDTO(@NonNull User user, @NonNull UUID inventoryId,
                                          @NonNull ItemRequestDTO dto, byte[] imageData) {
        Item item = createItem(user, inventoryId, dto, imageData);
        // New items have no claims
        return ItemResponseDTO.toDTO(item, 0, false, null, false);
    }

    public ItemResponseDTO updateItemDTO(@NonNull User user, @NonNull UUID inventoryId,
                                          @NonNull UUID itemId, @NonNull ItemRequestDTO dto) {
        Item item = updateItem(user, inventoryId, itemId, dto);
        return buildItemResponseDTO(item, user);
    }

    public ItemResponseDTO updateItemImageDTO(@NonNull User user, @NonNull UUID inventoryId,
                                               @NonNull UUID itemId, @NonNull byte[] imageData) {
        Item item = updateItemImage(user, inventoryId, itemId, imageData);
        return buildItemResponseDTO(item, user);
    }

    private ItemResponseDTO buildItemResponseDTO(Item item, User user) {
        List<ItemClaim> claims = claimRepository.findByItem(item);
        int claimCount = claims.size();

        ItemClaim assigned = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.ASSIGNED)
                .findFirst()
                .orElse(null);

        boolean isAssigned = assigned != null;
        String assignedToName = isAssigned
                ? assigned.getUser().getFirstName() + " " + assigned.getUser().getLastName()
                : null;

        boolean currentUserClaimed = claims.stream()
                .anyMatch(c -> c.getUser().equals(user));

        return ItemResponseDTO.toDTO(item, claimCount, isAssigned, assignedToName, currentUserClaimed);
    }
}
