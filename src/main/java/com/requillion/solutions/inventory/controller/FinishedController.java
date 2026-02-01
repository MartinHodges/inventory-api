package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.service.MemberService;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/{inventoryId}")
@RequiredArgsConstructor
@Slf4j
public class FinishedController {

    private final MemberService memberService;

    @PostMapping("/finished")
    public ResponseEntity<Void> markAsFinished(@PathVariable UUID inventoryId) {
        LoggerUtil.debug(log, "markAsFinished: inventory=%s", inventoryId);
        RequestContext context = UserContext.getContext();
        memberService.markAsFinished(context.getUser(), inventoryId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/members/{memberId}/finished")
    public ResponseEntity<Void> setMemberFinished(
            @PathVariable UUID inventoryId,
            @PathVariable UUID memberId,
            @RequestBody Map<String, Boolean> body) {
        LoggerUtil.debug(log, "setMemberFinished: inventory=%s, member=%s", inventoryId, memberId);
        RequestContext context = UserContext.getContext();
        boolean finished = body.getOrDefault("finished", false);
        memberService.setMemberFinished(context.getUser(), inventoryId, memberId, finished);
        return ResponseEntity.ok().build();
    }
}
