package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.InvitationRequestDTO;
import com.requillion.solutions.inventory.dto.InvitationResponseDTO;
import com.requillion.solutions.inventory.model.Invitation;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.InvitationService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponseDTO> createInvitation(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody InvitationRequestDTO dto) {
        LoggerUtil.debug(log, "createInvitation: inventory=%s, email=%s", inventoryId, dto.email());
        RequestContext context = UserContext.getContext();
        Invitation invitation = invitationService.createInvitation(
                context.getUser(), inventoryId, dto.email(), dto.role());
        return ResponseEntity.ok(InvitationResponseDTO.fromEntity(invitation));
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponseDTO>> getPendingInvitations(
            @PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getPendingInvitations: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        List<Invitation> invitations = invitationService.getPendingInvitations(
                context.getUser(), inventoryId);
        return ResponseEntity.ok(InvitationResponseDTO.fromEntities(invitations));
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable UUID inventoryId,
            @PathVariable UUID invitationId) {
        LoggerUtil.debug(log, "cancelInvitation: inventory=%s, invitation=%s", inventoryId, invitationId);
        RequestContext context = UserContext.getContext();
        invitationService.cancelInvitation(context.getUser(), invitationId);
        return ResponseEntity.noContent().build();
    }
}
