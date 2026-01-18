package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.dto.InventoryEventDTO;
import com.requillion.solutions.inventory.exception.BadInputException;
import com.requillion.solutions.inventory.model.User;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class InventoryEventService {

    private static final int MAX_CONNECTIONS_PER_USER = 5;

    private record EmitterEntry(UUID userId, SseEmitter emitter) {}

    private final Map<UUID, CopyOnWriteArrayList<EmitterEntry>> emittersByInventory = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> connectionsByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID inventoryId, User user) {
        // Check connection limit
        int currentConnections = connectionsByUser
                .computeIfAbsent(user.getId(), k -> new AtomicInteger(0))
                .incrementAndGet();

        if (currentConnections > MAX_CONNECTIONS_PER_USER) {
            connectionsByUser.get(user.getId()).decrementAndGet();
            throw new BadInputException(
                    "Too many active connections",
                    "User: %s, Connections: %d", user.getId(), currentConnections);
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        EmitterEntry entry = new EmitterEntry(user.getId(), emitter);

        emittersByInventory.computeIfAbsent(inventoryId, k -> new CopyOnWriteArrayList<>()).add(entry);

        emitter.onCompletion(() -> removeEmitter(inventoryId, entry));
        emitter.onTimeout(() -> removeEmitter(inventoryId, entry));
        emitter.onError(e -> removeEmitter(inventoryId, entry));

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("inventoryId", inventoryId.toString())));
        } catch (IOException e) {
            removeEmitter(inventoryId, entry);
        }

        LoggerUtil.info(log, "SSE subscription created for inventory %s by user %s", inventoryId, user.getId());
        return emitter;
    }

    public void publishEvent(InventoryEventDTO event) {
        List<EmitterEntry> entries = emittersByInventory.get(event.inventoryId());
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<EmitterEntry> deadEmitters = new ArrayList<>();

        for (EmitterEntry entry : entries) {
            try {
                entry.emitter().send(SseEmitter.event()
                        .name(event.type().toLowerCase())
                        .data(event));
            } catch (IOException e) {
                deadEmitters.add(entry);
            }
        }

        if (!deadEmitters.isEmpty()) {
            deadEmitters.forEach(e -> removeEmitter(event.inventoryId(), e));
        }

        LoggerUtil.debug(log, "Published %s event for inventory %s to %d subscribers",
                event.type(), event.inventoryId(), entries.size() - deadEmitters.size());
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeats() {
        emittersByInventory.forEach((inventoryId, entries) -> {
            List<EmitterEntry> deadEmitters = new ArrayList<>();
            for (EmitterEntry entry : entries) {
                try {
                    entry.emitter().send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", System.currentTimeMillis())));
                } catch (IOException e) {
                    deadEmitters.add(entry);
                }
            }
            deadEmitters.forEach(e -> removeEmitter(inventoryId, e));
        });
    }

    private void removeEmitter(UUID inventoryId, EmitterEntry entry) {
        List<EmitterEntry> entries = emittersByInventory.get(inventoryId);
        if (entries != null) {
            entries.remove(entry);
        }
        AtomicInteger userConnections = connectionsByUser.get(entry.userId());
        if (userConnections != null) {
            userConnections.decrementAndGet();
        }
        LoggerUtil.debug(log, "SSE connection closed for inventory %s, user %s", inventoryId, entry.userId());
    }
}
