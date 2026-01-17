package com.requillion.solutions.inventory.controller;

import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        LoggerUtil.debug(log, "Health check");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "inventory-api");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        LoggerUtil.debug(log, "Me endpoint");

        RequestContext context = UserContext.getContext();
        Map<String, Object> response = new HashMap<>();

        if (context != null && context.isAuthenticated()) {
            response.put("authenticated", true);
            response.put("id", context.getUser().getId());
            response.put("email", context.getUser().getEmail());
            response.put("firstName", context.getUser().getFirstName());
            response.put("lastName", context.getUser().getLastName());
        } else {
            response.put("authenticated", false);
        }

        return ResponseEntity.ok(response);
    }
}
