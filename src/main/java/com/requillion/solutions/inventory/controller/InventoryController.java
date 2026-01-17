package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.InventoryRequestDTO;
import com.requillion.solutions.inventory.dto.InventoryResponseDTO;
import com.requillion.solutions.inventory.dto.InventoryWithMeta;
import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.InventoryService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponseDTO>> getInventories() {
        LoggerUtil.debug(log, "getInventories");
        RequestContext context = UserContext.getContext();
        List<InventoryWithMeta> inventories = inventoryService.getInventories(context.getUser());
        return ResponseEntity.ok(InventoryResponseDTO.toDTO(inventories));
    }

    @GetMapping("/{inventoryId}")
    public ResponseEntity<InventoryResponseDTO> getInventory(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getInventory: %s", inventoryId);
        RequestContext context = UserContext.getContext();
        InventoryWithMeta inventory = inventoryService.getInventory(context.getUser(), inventoryId);
        return ResponseEntity.ok(InventoryResponseDTO.toDTO(
                inventory.inventory(), inventory.isOwner(), inventory.userRole(), inventory.itemCount()));
    }

    @PostMapping
    public ResponseEntity<InventoryResponseDTO> createInventory(
            @Valid @RequestBody InventoryRequestDTO dto) {
        LoggerUtil.debug(log, "createInventory: %s", dto.name());
        RequestContext context = UserContext.getContext();
        Inventory inventory = inventoryService.createInventory(context.getUser(), dto);
        return ResponseEntity.ok(InventoryResponseDTO.toDTO(
                inventory, true, null, 0));
    }

    @PutMapping("/{inventoryId}")
    public ResponseEntity<InventoryResponseDTO> updateInventory(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody InventoryRequestDTO dto) {
        LoggerUtil.debug(log, "updateInventory: %s", inventoryId);
        RequestContext context = UserContext.getContext();
        Inventory inventory = inventoryService.updateInventory(context.getUser(), inventoryId, dto);
        InventoryWithMeta meta = inventoryService.getInventory(context.getUser(), inventoryId);
        return ResponseEntity.ok(InventoryResponseDTO.toDTO(
                inventory, meta.isOwner(), meta.userRole(), meta.itemCount()));
    }

    @DeleteMapping("/{inventoryId}")
    public ResponseEntity<Void> deleteInventory(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "deleteInventory: %s", inventoryId);
        RequestContext context = UserContext.getContext();
        inventoryService.deleteInventory(context.getUser(), inventoryId);
        return ResponseEntity.noContent().build();
    }
}
