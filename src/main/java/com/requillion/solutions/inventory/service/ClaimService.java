package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.AllClaimsResponseDTO;
import com.requillion.solutions.inventory.dto.ClaimedItemDTO;
import com.requillion.solutions.inventory.dto.InventoryEventDTO;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClaimService {

    private final ItemClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMemberRepository memberRepository;
    private final InventoryEventService eventService;

    public ItemClaim createClaim(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        // Check user can claim (must be CLAIMANT or ADMIN role, or owner)
        if (!canUserClaim(user, item.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to claim items in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        // Finished claimants cannot create new claims (admins can)
        checkNotFinishedClaimant(user, item.getInventory());

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

        eventService.publishEvent(InventoryEventDTO.claimCreated(inventoryId, itemId, claim.getId()));

        return claim;
    }

    public void withdrawClaim(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID itemId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        // Finished claimants cannot withdraw claims (admins can)
        checkNotFinishedClaimant(user, item.getInventory());

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

        UUID claimId = claim.getId();
        claimRepository.delete(claim);
        LoggerUtil.info(log, "User %s withdrew interest in item %s", user.getId(), itemId);

        eventService.publishEvent(InventoryEventDTO.claimDeleted(inventoryId, itemId, claimId));
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
        eventService.publishEvent(InventoryEventDTO.itemAssigned(inventoryId, itemId, claim.getId()));

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
        eventService.publishEvent(InventoryEventDTO.itemUnassigned(inventoryId, itemId));
    }

    public void deleteClaim(@NonNull User user, @NonNull UUID inventoryId,
                            @NonNull UUID itemId, @NonNull UUID claimId) {
        Item item = getItemWithAccess(user, inventoryId, itemId);

        if (!canUserManageInventory(user, item.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to remove claims in this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        ItemClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException(
                        "Claim not found",
                        "Claim: %s", claimId));

        if (!claim.getItem().equals(item)) {
            throw new BadInputException(
                    "Claim does not belong to this item",
                    "Claim: %s, Item: %s", claimId, itemId);
        }

        claimRepository.delete(claim);
        LoggerUtil.info(log, "Admin %s removed claim %s from item %s", user.getId(), claimId, itemId);

        eventService.publishEvent(InventoryEventDTO.claimDeleted(inventoryId, itemId, claimId));
    }

    public List<AllClaimsResponseDTO> getAllClaims(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!canUserManageInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to view all claims",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        // Get all active admins and claimants
        List<InventoryMember> members = memberRepository.findByInventoryAndStatusAndRoleIn(
                inventory, MemberStatus.ACTIVE, List.of(MemberRole.ADMIN, MemberRole.CLAIMANT));

        // Include the owner
        User owner = inventory.getOwner();

        // Get all claims for this inventory
        List<ItemClaim> allClaims = claimRepository.findAllByInventoryId(inventoryId);

        // Group claims by user ID
        Map<UUID, List<ItemClaim>> claimsByUser = allClaims.stream()
                .collect(Collectors.groupingBy(c -> c.getUser().getId()));

        // Count claims per item
        Map<UUID, Long> claimCountByItem = allClaims.stream()
                .collect(Collectors.groupingBy(c -> c.getItem().getId(), Collectors.counting()));

        // Build response: owner first, then members sorted by name
        List<AllClaimsResponseDTO> result = new ArrayList<>();

        // Add owner (owner never has finished status, no member record)
        result.add(buildMemberClaims(owner.getId(), null,
                owner.getFirstName() + " " + owner.getLastName(),
                null, false, claimsByUser, claimCountByItem));

        // Add members (excluding owner to avoid duplicates), sorted by name
        members.stream()
                .filter(member -> !member.getUser().equals(owner))
                .sorted((a, b) -> {
                    String nameA = a.getUser().getFirstName() + " " + a.getUser().getLastName();
                    String nameB = b.getUser().getFirstName() + " " + b.getUser().getLastName();
                    return nameA.compareToIgnoreCase(nameB);
                })
                .forEach(member -> result.add(buildMemberClaims(
                        member.getUser().getId(), member.getId(),
                        member.getUser().getFirstName() + " " + member.getUser().getLastName(),
                        member.getRole(), member.getFinishedAt() != null, claimsByUser, claimCountByItem)));

        return result;
    }

    private AllClaimsResponseDTO buildMemberClaims(UUID userId, UUID memberId, String userName,
                                                    MemberRole role, boolean isFinished,
                                                    Map<UUID, List<ItemClaim>> claimsByUser,
                                                    Map<UUID, Long> claimCountByItem) {
        List<ItemClaim> userClaims = claimsByUser.getOrDefault(userId, List.of());
        List<ClaimedItemDTO> claimedItems = userClaims.stream()
                .map(claim -> ClaimedItemDTO.fromClaim(claim,
                        claimCountByItem.getOrDefault(claim.getItem().getId(), 0L).intValue()))
                .toList();
        return new AllClaimsResponseDTO(userId, memberId, userName, role, isFinished, claimedItems);
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

    private void checkNotFinishedClaimant(User user, Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return; // Owner is never restricted
        }
        memberRepository.findByInventoryAndUser(inventory, user)
                .filter(m -> m.getRole() == MemberRole.CLAIMANT && m.getFinishedAt() != null)
                .ifPresent(m -> {
                    throw new BadInputException(
                            "You have marked yourself as finished and can no longer change claims",
                            "Member: %s, Inventory: %s", m.getId(), inventory.getId());
                });
    }
}
