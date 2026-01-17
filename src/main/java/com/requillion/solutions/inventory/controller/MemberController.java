package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.MemberResponseDTO;
import com.requillion.solutions.inventory.dto.MemberUpdateDTO;
import com.requillion.solutions.inventory.model.InventoryMember;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.MemberService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<List<MemberResponseDTO>> getMembers(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getMembers: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        List<InventoryMember> members = memberService.getMembers(context.getUser(), inventoryId);
        return ResponseEntity.ok(MemberResponseDTO.fromEntities(members));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponseDTO> updateMember(
            @PathVariable UUID inventoryId,
            @PathVariable UUID memberId,
            @Valid @RequestBody MemberUpdateDTO dto) {
        LoggerUtil.debug(log, "updateMember: inventory=%s, member=%s", inventoryId, memberId);
        RequestContext context = UserContext.getContext();

        InventoryMember member = null;
        if (dto.role() != null) {
            member = memberService.updateMemberRole(context.getUser(), inventoryId, memberId, dto.role());
        }
        if (dto.status() != null) {
            member = memberService.updateMemberStatus(context.getUser(), inventoryId, memberId, dto.status());
        }

        return ResponseEntity.ok(MemberResponseDTO.fromEntity(member));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID inventoryId,
            @PathVariable UUID memberId) {
        LoggerUtil.debug(log, "removeMember: inventory=%s, member=%s", inventoryId, memberId);
        RequestContext context = UserContext.getContext();
        memberService.removeMember(context.getUser(), inventoryId, memberId);
        return ResponseEntity.noContent().build();
    }
}
