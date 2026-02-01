package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.AllClaimsResponseDTO;
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
@RequestMapping("/api/v1/inventories/{inventoryId}/claims")
@RequiredArgsConstructor
@Slf4j
public class AllClaimsController {

    private final ClaimService claimService;

    @GetMapping("/all")
    public ResponseEntity<List<AllClaimsResponseDTO>> getAllClaims(
            @PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getAllClaims: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        List<AllClaimsResponseDTO> claims = claimService.getAllClaims(context.getUser(), inventoryId);
        return ResponseEntity.ok(claims);
    }
}
