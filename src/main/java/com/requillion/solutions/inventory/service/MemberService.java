package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.*;
import com.requillion.solutions.inventory.repository.InventoryMemberRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMemberRepository memberRepository;
    private final EmailService emailService;

    public List<InventoryMember> getMembers(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = getInventoryWithAdminAccess(user, inventoryId);
        return memberRepository.findByInventoryAndStatus(inventory, MemberStatus.ACTIVE);
    }

    public InventoryMember updateMemberRole(@NonNull User user, @NonNull UUID inventoryId,
                                             @NonNull UUID memberId, @NonNull MemberRole newRole) {
        Inventory inventory = getInventoryWithAdminAccess(user, inventoryId);

        InventoryMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(
                        "Member not found",
                        "Member: %s", memberId));

        if (!member.getInventory().equals(inventory)) {
            throw new NotFoundException(
                    "Member not found in this inventory",
                    "Inventory: %s, Member: %s", inventoryId, memberId);
        }

        member.setRole(newRole);
        member = memberRepository.saveAndFlush(member);

        LoggerUtil.info(log, "Updated member %s role to %s in inventory %s",
                memberId, newRole, inventoryId);

        return member;
    }

    public InventoryMember updateMemberStatus(@NonNull User user, @NonNull UUID inventoryId,
                                               @NonNull UUID memberId, @NonNull MemberStatus newStatus) {
        Inventory inventory = getInventoryWithAdminAccess(user, inventoryId);

        InventoryMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(
                        "Member not found",
                        "Member: %s", memberId));

        if (!member.getInventory().equals(inventory)) {
            throw new NotFoundException(
                    "Member not found in this inventory",
                    "Inventory: %s, Member: %s", inventoryId, memberId);
        }

        member.setStatus(newStatus);
        member = memberRepository.save(member);

        LoggerUtil.info(log, "Updated member %s status to %s in inventory %s",
                memberId, newStatus, inventoryId);

        return member;
    }

    public void activateMemberOnFirstAccess(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        memberRepository.findByInventoryAndUser(inventory, user)
                .filter(member -> member.getStatus() == MemberStatus.PENDING)
                .ifPresent(member -> {
                    member.setStatus(MemberStatus.ACTIVE);
                    memberRepository.save(member);
                    LoggerUtil.info(log, "Activated member %s for inventory %s on first access",
                            user.getId(), inventoryId);
                });
    }

    public void removeMember(@NonNull User user, @NonNull UUID inventoryId, @NonNull UUID memberId) {
        Inventory inventory = getInventoryWithAdminAccess(user, inventoryId);

        InventoryMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(
                        "Member not found",
                        "Member: %s", memberId));

        if (!member.getInventory().equals(inventory)) {
            throw new NotFoundException(
                    "Member not found in this inventory",
                    "Inventory: %s, Member: %s", inventoryId, memberId);
        }

        // Cannot remove the owner
        if (member.getUser().equals(inventory.getOwner())) {
            throw new BadInputException(
                    "Cannot remove the inventory owner",
                    "Inventory: %s, Owner: %s", inventoryId, member.getUser().getId());
        }

        memberRepository.delete(member);
        LoggerUtil.info(log, "Removed member %s from inventory %s", memberId, inventoryId);
    }

    public void markAsFinished(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (inventory.getOwner().equals(user)) {
            throw new BadInputException(
                    "Owners cannot mark themselves as finished",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        InventoryMember member = memberRepository.findByInventoryAndUser(inventory, user)
                .orElseThrow(() -> new NotAuthorizedException(
                        "You are not a member of this inventory",
                        "Inventory: %s, User: %s", inventoryId, user.getId()));

        if (member.getFinishedAt() == null) {
            member.setFinishedAt(Instant.now());
            memberRepository.save(member);
            LoggerUtil.info(log, "Member %s marked as finished in inventory %s", user.getId(), inventoryId);
        }
    }

    public void setMemberFinished(@NonNull User user, @NonNull UUID inventoryId,
                                   @NonNull UUID memberId, boolean finished) {
        Inventory inventory = getInventoryWithAdminAccess(user, inventoryId);

        InventoryMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(
                        "Member not found",
                        "Member: %s", memberId));

        if (!member.getInventory().equals(inventory)) {
            throw new NotFoundException(
                    "Member not found in this inventory",
                    "Inventory: %s, Member: %s", inventoryId, memberId);
        }

        if (finished && member.getFinishedAt() == null) {
            member.setFinishedAt(Instant.now());
            memberRepository.save(member);
            LoggerUtil.info(log, "Admin %s marked member %s as finished in inventory %s",
                    user.getId(), memberId, inventoryId);
        } else if (!finished && member.getFinishedAt() != null) {
            member.setFinishedAt(null);
            memberRepository.save(member);
            LoggerUtil.info(log, "Admin %s reset finished for member %s in inventory %s",
                    user.getId(), memberId, inventoryId);
        }
    }

    private Inventory getInventoryWithAdminAccess(User user, UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!canUserManageInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to manage members for this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        return inventory;
    }

    private boolean canUserManageInventory(User user, Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.existsByInventoryAndUserAndStatusAndRoleIn(
                inventory, user, MemberStatus.ACTIVE, List.of(MemberRole.ADMIN));
    }
}
