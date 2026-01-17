package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.InventoryRequestDTO;
import com.requillion.solutions.inventory.dto.InventoryWithMeta;
import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.*;
import com.requillion.solutions.inventory.repository.InventoryMemberRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.repository.ItemRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMemberRepository memberRepository;
    private final ItemRepository itemRepository;

    public List<InventoryWithMeta> getInventories(@NonNull User user) {
        List<InventoryWithMeta> result = new ArrayList<>();

        // Get owned inventories
        List<Inventory> owned = inventoryRepository.findByOwner(user);
        for (Inventory inv : owned) {
            int count = (int) itemRepository.countByInventoryAndIsDeletedFalse(inv);
            result.add(new InventoryWithMeta(inv, true, MemberRole.ADMIN, count));
        }

        // Get inventories user is a member of (with ACTIVE status)
        List<InventoryMember> memberships = memberRepository.findByUser(user);
        for (InventoryMember membership : memberships) {
            if (membership.getStatus() == MemberStatus.ACTIVE) {
                Inventory inv = membership.getInventory();
                if (!inv.getOwner().equals(user)) { // Don't duplicate owned inventories
                    int count = (int) itemRepository.countByInventoryAndIsDeletedFalse(inv);
                    result.add(new InventoryWithMeta(inv, false, membership.getRole(), count));
                }
            }
        }

        LoggerUtil.info(log, "Retrieved %d inventories for user %s", result.size(), user.getId());
        return result;
    }

    public InventoryWithMeta getInventory(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        // Check access
        boolean isOwner = inventory.getOwner().equals(user);
        MemberRole role = null;

        if (!isOwner) {
            InventoryMember membership = memberRepository.findByInventoryAndUser(inventory, user)
                    .orElseThrow(() -> new NotAuthorizedException(
                            "You do not have access to this inventory",
                            "Inventory: %s, User: %s", inventoryId, user.getId()));

            if (membership.getStatus() != MemberStatus.ACTIVE) {
                throw new NotAuthorizedException(
                        "Your access to this inventory is pending approval",
                        "Inventory: %s, User: %s, Status: %s",
                        inventoryId, user.getId(), membership.getStatus());
            }
            role = membership.getRole();
        }

        int itemCount = (int) itemRepository.countByInventoryAndIsDeletedFalse(inventory);
        return new InventoryWithMeta(inventory, isOwner, role, itemCount);
    }

    public Inventory createInventory(@NonNull User owner, @NonNull InventoryRequestDTO dto) {
        // Check for duplicate name
        if (inventoryRepository.findByOwnerAndName(owner, dto.name()).isPresent()) {
            throw new BadInputException(
                    "You already have an inventory named '" + dto.name() + "'",
                    "Inventory name: %s, Owner: %s", dto.name(), owner.getId());
        }

        Inventory inventory = new Inventory();
        inventory.setOwner(owner);
        inventory.setName(dto.name());
        inventory.setDescription(dto.description());

        inventory = inventoryRepository.save(inventory);
        LoggerUtil.info(log, "Created inventory %s for user %s", inventory.getId(), owner.getId());

        return inventory;
    }

    public Inventory updateInventory(@NonNull User user, @NonNull UUID inventoryId, @NonNull InventoryRequestDTO dto) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        // Only owner can update
        if (!inventory.getOwner().equals(user)) {
            throw new NotAuthorizedException(
                    "Only the inventory owner can update it",
                    "Inventory: %s, Owner: %s, User: %s",
                    inventoryId, inventory.getOwner().getId(), user.getId());
        }

        // Check for duplicate name (excluding current inventory)
        inventoryRepository.findByOwnerAndName(user, dto.name())
                .filter(existing -> !existing.getId().equals(inventoryId))
                .ifPresent(existing -> {
                    throw new BadInputException(
                            "You already have an inventory named '" + dto.name() + "'",
                            "Inventory name: %s, Owner: %s", dto.name(), user.getId());
                });

        inventory.setName(dto.name());
        inventory.setDescription(dto.description());

        inventory = inventoryRepository.save(inventory);
        LoggerUtil.info(log, "Updated inventory %s", inventory.getId());

        return inventory;
    }

    public void deleteInventory(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findByOwnerAndId(user, inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found or you are not the owner",
                        "Inventory: %s, User: %s", inventoryId, user.getId()));

        inventoryRepository.delete(inventory);
        LoggerUtil.info(log, "Deleted inventory %s", inventoryId);
    }

    public boolean canUserViewInventory(@NonNull User user, @NonNull Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.findByInventoryAndUser(inventory, user)
                .map(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElse(false);
    }

    public boolean canUserEditInventory(@NonNull User user, @NonNull Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.existsByInventoryAndUserAndStatusAndRoleIn(
                inventory, user, MemberStatus.ACTIVE, List.of(MemberRole.ADMIN));
    }
}
