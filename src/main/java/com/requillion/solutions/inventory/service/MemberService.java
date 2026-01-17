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
        return memberRepository.findByInventory(inventory);
    }

    public List<InventoryMember> getPendingMembers(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = getInventoryWithAdminAccess(user, inventoryId);
        return memberRepository.findByInventoryAndStatus(inventory, MemberStatus.PENDING);
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
        member = memberRepository.save(member);

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

        MemberStatus previousStatus = member.getStatus();
        member.setStatus(newStatus);
        member = memberRepository.save(member);

        LoggerUtil.info(log, "Updated member %s status from %s to %s in inventory %s",
                memberId, previousStatus, newStatus, inventoryId);

        // Send notification if approved
        if (previousStatus == MemberStatus.PENDING && newStatus == MemberStatus.ACTIVE) {
            emailService.sendMemberApproved(member, inventory.getName());
        }

        return member;
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
