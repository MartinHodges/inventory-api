package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.dto.ItemRequestDTO;
import com.requillion.solutions.inventory.dto.ItemResponseDTO;
import com.requillion.solutions.inventory.dto.ItemWithThumbnailDTO;
import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.ItemService;
import com.requillion.solutions.inventory.util.LoggerUtil;
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
@RequestMapping("/api/v1/inventories/{inventoryId}/categories/{categoryId}/items")
@RequiredArgsConstructor
@Slf4j
public class CategoryItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<List<ItemWithThumbnailDTO>> getItems(
            @PathVariable UUID inventoryId,
            @PathVariable UUID categoryId) {
        LoggerUtil.debug(log, "getItems: inventory=%s, category=%s", inventoryId, categoryId);
        RequestContext context = UserContext.getContext();
        List<ItemWithThumbnailDTO> items = itemService.getItemsWithThumbnailsByCategory(
                context.getUser(), inventoryId, categoryId);
        return ResponseEntity.ok(items);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponseDTO> createItem(
            @PathVariable UUID inventoryId,
            @PathVariable UUID categoryId,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        LoggerUtil.debug(log, "createItem: inventory=%s, category=%s", inventoryId, categoryId);
        RequestContext context = UserContext.getContext();

        ItemRequestDTO dto = new ItemRequestDTO(description);
        byte[] imageData = image != null && !image.isEmpty() ? image.getBytes() : null;

        ItemResponseDTO item = itemService.createItemInCategoryDTO(
                context.getUser(), inventoryId, categoryId, dto, imageData);
        return ResponseEntity.ok(item);
    }
}
