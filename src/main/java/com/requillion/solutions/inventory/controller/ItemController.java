package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.ItemRequestDTO;
import com.requillion.solutions.inventory.dto.ItemResponseDTO;
import com.requillion.solutions.inventory.dto.ItemWithThumbnailDTO;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.ItemService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<List<ItemWithThumbnailDTO>> getItems(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getItems: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        List<ItemWithThumbnailDTO> items = itemService.getItemsWithThumbnails(context.getUser(), inventoryId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/my-claims")
    public ResponseEntity<List<ItemWithThumbnailDTO>> getMyClaimedItems(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "getMyClaimedItems: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        List<ItemWithThumbnailDTO> items = itemService.getClaimedItemsWithThumbnails(
                context.getUser(), inventoryId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponseDTO> getItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "getItem: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        ItemResponseDTO item = itemService.getItemDTO(context.getUser(), inventoryId, itemId);
        return ResponseEntity.ok(item);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponseDTO> createItem(
            @PathVariable UUID inventoryId,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        LoggerUtil.debug(log, "createItem: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();

        ItemRequestDTO dto = new ItemRequestDTO(description);
        byte[] imageData = image != null && !image.isEmpty() ? image.getBytes() : null;

        ItemResponseDTO item = itemService.createItemDTO(context.getUser(), inventoryId, dto, imageData);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponseDTO> updateItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemRequestDTO dto) {
        LoggerUtil.debug(log, "updateItem: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        ItemResponseDTO item = itemService.updateItemDTO(context.getUser(), inventoryId, itemId, dto);
        return ResponseEntity.ok(item);
    }

    @PutMapping(value = "/{itemId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponseDTO> updateItemImage(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId,
            @RequestParam("image") MultipartFile image) throws IOException {
        LoggerUtil.debug(log, "updateItemImage: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        ItemResponseDTO item = itemService.updateItemImageDTO(context.getUser(), inventoryId, itemId, image.getBytes());
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "deleteItem: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        itemService.deleteItem(context.getUser(), inventoryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{itemId}/collect")
    public ResponseEntity<Void> collectItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "collectItem: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        itemService.collectItem(context.getUser(), inventoryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{itemId}/uncollect")
    public ResponseEntity<Void> uncollectItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "uncollectItem: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        itemService.uncollectItem(context.getUser(), inventoryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{itemId}/image")
    public ResponseEntity<byte[]> getItemImage(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "getItemImage: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        byte[] image = itemService.getItemImage(context.getUser(), inventoryId, itemId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }

    @GetMapping("/{itemId}/thumbnail")
    public ResponseEntity<byte[]> getItemThumbnail(
            @PathVariable UUID inventoryId,
            @PathVariable UUID itemId) {
        LoggerUtil.debug(log, "getItemThumbnail: inventory=%s, item=%s", inventoryId, itemId);
        RequestContext context = UserContext.getContext();
        byte[] thumbnail = itemService.getItemThumbnail(context.getUser(), inventoryId, itemId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(thumbnail);
    }
}
