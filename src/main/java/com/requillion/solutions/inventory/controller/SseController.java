package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.model.User;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.InventoryEventService;
import com.requillion.solutions.inventory.service.InventoryService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}/events")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final InventoryEventService eventService;
    private final InventoryService inventoryService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> subscribe(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "SSE subscribe request for inventory %s", inventoryId);
        RequestContext context = UserContext.getContext();
        User user = context.getUser();

        if (!inventoryService.canUserViewInventory(user, inventoryId)) {
            LoggerUtil.error(log, "SSE subscribe denied: Inventory: %s, User: %s", inventoryId, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"FORBIDDEN\",\"message\":\"You do not have access to this inventory\",\"status\":403}");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(eventService.subscribe(inventoryId, user));
    }
}
