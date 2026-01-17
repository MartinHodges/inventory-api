package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.ClaimResponseDTO;
import com.requillion.solutions.inventory.model.ItemClaim;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.ClaimService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}/items/{itemId}/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponseDTO> createClaim(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "createClaim: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        ItemClaim claim = claimService.createClaim(context.getUser(), inventoryId, itemId);
        return ResponseEntity.ok(ClaimResponseDTO.fromEntity(claim));
    }

    @DeleteMapping("/mine")
    public ResponseEntity<Void> withdrawClaim(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "withdrawClaim: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        claimService.withdrawClaim(context.getUser(), inventoryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponseDTO>> getClaims(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "getClaims: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        List<ItemClaim> claims = claimService.getClaims(context.getUser(), inventoryId, itemId);
        return ResponseEntity.ok(ClaimResponseDTO.fromEntities(claims));
    }

    @PutMapping("/{claimId}/assign")
    public ResponseEntity<ClaimResponseDTO> assignItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId,
            @PathVariable UUID claimId) {
        LoggerUtil.debug(log, "assignItem: inventory=%s, item=%s, claim=%s", inventoryId, itemId, claimId);
        RequestContext context = UserContext.getContext();
        ItemClaim claim = claimService.assignItem(context.getUser(), inventoryId, itemId, claimId);
        return ResponseEntity.ok(ClaimResponseDTO.fromEntity(claim));
    }

    @DeleteMapping("/assignment")
    public ResponseEntity<Void> unassignItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "unassignItem: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        claimService.unassignItem(context.getUser(), inventoryId, itemId);
        return ResponseEntity.noContent().build();
    }
}
