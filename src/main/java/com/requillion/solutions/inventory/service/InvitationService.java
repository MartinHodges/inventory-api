package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.exception.NotAuthorizedException;
import com.requillion.solutions.inventory.exception.NotFoundException;
import com.requillion.solutions.inventory.model.*;
import com.requillion.solutions.inventory.repository.InvitationRepository;
import com.requillion.solutions.inventory.repository.InventoryMemberRepository;
import com.requillion.solutions.inventory.repository.InventoryRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvitationService {

    private static final int TOKEN_LENGTH = 32;
    private static final int EXPIRY_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMemberRepository memberRepository;
    private final EmailService emailService;

    public Invitation createInvitation(@NonNull User user, @NonNull UUID inventoryId,
                                        @NonNull String email, @NonNull MemberRole role) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        // Check user has admin access
        if (!canUserManageInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to invite users to this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        // Check if user is already a member
        memberRepository.findByInventoryAndUser(inventory, null)
                .ifPresent(m -> {
                    // We'll check by email after the fact since user lookup is different
                });

        // Check for existing pending invitation
        invitationRepository.findPendingByInventoryAndEmail(inventory, email)
                .ifPresent(existing -> {
                    throw new BadInputException(
                            "An invitation has already been sent to this email",
                            "Inventory: %s, Email: %s", inventoryId, email);
                });

        // Create invitation
        Invitation invitation = new Invitation();
        invitation.setInventory(inventory);
        invitation.setEmail(email.toLowerCase().trim());
        invitation.setRole(role);
        invitation.setToken(generateToken());
        invitation.setInvitedBy(user);
        invitation.setExpiresAt(Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS));

        invitation = invitationRepository.save(invitation);
        LoggerUtil.info(log, "Created invitation %s for %s to inventory %s",
                invitation.getId(), email, inventoryId);

        // Send email
        String inviterName = user.getFirstName() + " " + user.getLastName();
        emailService.sendInvitation(invitation, inventory.getName(), inviterName);

        return invitation;
    }

    public Invitation getInvitationByToken(@NonNull String token) {
        return invitationRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException(
                        "Invitation not found or has expired",
                        "Token: %s", token));
    }

    public InventoryMember acceptInvitation(@NonNull String token, @NonNull User user) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException(
                        "Invitation not found",
                        "Token: %s", token));

        if (invitation.isAccepted()) {
            throw new BadInputException(
                    "This invitation has already been accepted",
                    "Invitation: %s", invitation.getId());
        }

        if (invitation.isExpired()) {
            throw new BadInputException(
                    "This invitation has expired",
                    "Invitation: %s, Expired: %s", invitation.getId(), invitation.getExpiresAt());
        }

        // Check user email matches (case-insensitive)
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new NotAuthorizedException(
                    "This invitation was sent to a different email address",
                    "Invitation email: %s, User email: %s", invitation.getEmail(), user.getEmail());
        }

        // Check if already a member
        Inventory inventory = invitation.getInventory();
        if (memberRepository.findByInventoryAndUser(inventory, user).isPresent()) {
            throw new BadInputException(
                    "You are already a member of this inventory",
                    "Inventory: %s, User: %s", inventory.getId(), user.getId());
        }

        // Mark invitation as accepted
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        // Create membership with PENDING status
        InventoryMember member = new InventoryMember();
        member.setInventory(inventory);
        member.setUser(user);
        member.setRole(invitation.getRole());
        member.setStatus(MemberStatus.PENDING);
        member = memberRepository.save(member);

        LoggerUtil.info(log, "User %s accepted invitation %s to inventory %s",
                user.getId(), invitation.getId(), inventory.getId());

        // Notify admins
        notifyAdminsOfNewMember(member, inventory);

        return member;
    }

    public List<Invitation> getPendingInvitations(@NonNull User user, @NonNull UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventory not found",
                        "Inventory: %s", inventoryId));

        if (!canUserManageInventory(user, inventory)) {
            throw new NotAuthorizedException(
                    "You do not have permission to view invitations for this inventory",
                    "Inventory: %s, User: %s", inventoryId, user.getId());
        }

        return invitationRepository.findPendingByInventory(inventory);
    }

    public void cancelInvitation(@NonNull User user, @NonNull UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException(
                        "Invitation not found",
                        "Invitation: %s", invitationId));

        if (!canUserManageInventory(user, invitation.getInventory())) {
            throw new NotAuthorizedException(
                    "You do not have permission to cancel this invitation",
                    "Invitation: %s, User: %s", invitationId, user.getId());
        }

        invitationRepository.delete(invitation);
        LoggerUtil.info(log, "Cancelled invitation %s", invitationId);
    }

    private boolean canUserManageInventory(User user, Inventory inventory) {
        if (inventory.getOwner().equals(user)) {
            return true;
        }
        return memberRepository.existsByInventoryAndUserAndStatusAndRoleIn(
                inventory, user, MemberStatus.ACTIVE, List.of(MemberRole.ADMIN));
    }

    private void notifyAdminsOfNewMember(InventoryMember newMember, Inventory inventory) {
        // Notify owner
        emailService.sendNewMemberNotification(
                newMember,
                inventory.getOwner().getEmail(),
                inventory.getName()
        );

        // Notify other admins
        List<InventoryMember> admins = memberRepository.findByInventoryAndRoleAndStatus(
                inventory, MemberRole.ADMIN, MemberStatus.ACTIVE);
        for (InventoryMember admin : admins) {
            if (!admin.getUser().equals(newMember.getUser())) {
                emailService.sendNewMemberNotification(
                        newMember,
                        admin.getUser().getEmail(),
                        inventory.getName()
                );
            }
        }
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
