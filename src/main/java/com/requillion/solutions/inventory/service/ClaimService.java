package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.*;
import com.requillion.solutions.inventory.repository.InventoryMemberRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.repository.ItemClaimRepository;
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
public class ClaimService {

    private final ItemClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMemberRepository memberRepository;

    public ItemClaim createClaim(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        // Check user can claim (must be CLAIMANT or ADMIN role, or owner)
        if (!canUserClaim(user, item.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to claim items in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        // Check if user already claimed this item
        if (claimRepository.findByItemAndUser(item, user).isPresent()) {
            throw new BadInputException(
                    "You have already expressed interest in this item",
                    "Item: %s, User: %s", itemId, user.getId());
        }

        ItemClaim claim = new ItemClaim();
        claim.setItem(item);
        claim.setUser(user);
        claim.setStatus(ClaimStatus.INTERESTED);

        claim = claimRepository.save(claim);
        LoggerUtil.info(log, "User %s expressed interest in item %s", user.getId(), itemId);

        return claim;
    }

    public void withdrawClaim(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        ItemClaim claim = claimRepository.findByItemAndUser(item, user)
                .orElseThrow(() -> new NotFoundException(
                        "You have not expressed interest in this item",
                        "Item: %s, User: %s", itemId, user.getId()));

        // Cannot withdraw if already assigned
        if (claim.getStatus() == ClaimStatus.ASSIGNED) {
            throw new BadInputException(
                    "Cannot withdraw - this item has been assigned to you",
                    "Claim: %s", claim.getId());
        }

        claimRepository.delete(claim);
        LoggerUtil.info(log, "User %s withdrew interest in item %s", user.getId(), itemId);
    }

    public List<ItemClaim> getClaims(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        // Only admins/owners can see all claims
        if (!canUserViewInventory(user, item.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to view claims for this item",
                    "Item: %s, User: %s", itemId, user.getId());
        }

        return claimRepository.findByItem(item);
    }

    public ItemClaim assignItem(@NonNull User user, @NonNull UUID inventoryId,
                                 @NonNull UUID itemId, @NonNull UUID claimId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        if (!canUserManageInventory(user, item.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to assign items in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        // Check if item is already assigned
        claimRepository.findByItemAndStatus(item, ClaimStatus.ASSIGNED)
                .ifPresent(existing -> {
                    throw new BadInputException(
                            "This item is already assigned to " +
                            existing.getUser().getFirstName() + " " + existing.getUser().getLastName(),
                            "Item: %s, Existing claim: %s", itemId, existing.getId());
                });

        ItemClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException(
                        "Claim not found",
                        "Claim: %s", claimId));

        if (!claim.getItem().equals(item)) {
            throw new BadInputException(
                    "Claim does not belong to this item",
                    "Claim: %s, Item: %s", claimId, itemId);
        }

        claim.setStatus(ClaimStatus.ASSIGNED);
        claim = claimRepository.save(claim);

        LoggerUtil.info(log, "Item %s assigned to user %s", itemId, claim.getUser().getId());
        return claim;
    }

    public void unassignItem(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        if (!canUserManageInventory(user, item.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to unassign items in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        ItemClaim assignedClaim = claimRepository.findByItemAndStatus(item, ClaimStatus.ASSIGNED)
                .orElseThrow(() -> new NotFoundException(
                        "This item is not assigned to anyone",
                        "Item: %s", itemId));

        assignedClaim.setStatus(ClaimStatus.INTERESTED);
        claimRepository.save(assignedClaim);

        LoggerUtil.info(log, "Item %s unassigned from user %s", itemId, assignedClaim.getUser().getId());
    }

    private Item getItemWithAccess(User user, UUID inventoryId, UUID itemId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!canUserViewInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have access to this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        return itemRepository.findByInventoryAndIdAndIsDeletedFalse(inventory, itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Item not found",
                        "Item: %s, Inventory: %s", itemId, inventoryId));
    }

    private boolean canUserViewInventory(User user, Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.findByInventoryAndUser(inventory, user)
                .map(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElse(false);
    }

    private boolean canUserClaim(User user, Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.existsByInventoryAndUserAndStatusAndRoleIn(
                inventory, user, MemberStatus.ACTIVE,
                List.of(MemberRole.ADMIN, MemberRole.CLAIMANT));
    }

    private boolean canUserManageInventory(User user, Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.existsByInventoryAndUserAndStatusAndRoleIn(
                inventory, user, MemberStatus.ACTIVE, List.of(MemberRole.ADMIN));
    }
}
