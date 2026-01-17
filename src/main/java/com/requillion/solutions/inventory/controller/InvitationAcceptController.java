package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.InvitationDetailsDTO;
import com.requillion.solutions.inventory.dto.MemberResponseDTO;
import com.requillion.solutions.inventory.model.Invitation;
import com.requillion.solutions.inventory.model.InventoryMember;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.InvitationService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationAcceptController {

    private final InvitationService invitationService;

    @GetMapping("/{token}")
    public ResponseEntity<InvitationDetailsDTO> getInvitationDetails(@PathVariable String token) {
        LoggerUtil.debug(log, "getInvitationDetails: token=%s", token.substring(0, Math.min(8, token.length())) + "...");
        Invitation invitation = invitationService.getInvitationByToken(token);
        return ResponseEntity.ok(InvitationDetailsDTO.fromEntity(invitation));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<MemberResponseDTO> acceptInvitation(@PathVariable String token) {
        LoggerUtil.debug(log, "acceptInvitation: token=%s", token.substring(0, Math.min(8, token.length())) + "...");
        RequestContext context = UserContext.getContext();
        InventoryMember member = invitationService.acceptInvitation(token, context.getUser());
        return ResponseEntity.ok(MemberResponseDTO.fromEntity(member));
    }
}
