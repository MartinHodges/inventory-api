package com.requillion.solutions.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.requillion.solutions.inventory.util.LoggerUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String X_USERINFO_HEADER = "x-userinfo";
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UUID requestId = UUID.randomUUID();
        UserInfo userInfo = null;

        try {
            String userInfoHeader = request.getHeader(X_USERINFO_HEADER);
            if (userInfoHeader != null && !userInfoHeader.isEmpty()) {
                try {
                    String decodedUserInfo = new String(
                            Base64.getDecoder().decode(userInfoHeader),
                            StandardCharsets.UTF_8
                    );
                    userInfo = objectMapper.readValue(decodedUserInfo, UserInfo.class);
                } catch (Exception e) {
                    LoggerUtil.warn(log, "Failed to decode x-userinfo header: %s", e.getMessage());
                }
            }

            RequestContext context = new RequestContext(requestId, userInfo);
            UserContext.setContext(context);

            LoggerUtil.debug(log, "Request started: %s %s (user: %s)",
                    request.getMethod(),
                    request.getRequestURI(),
                    userInfo != null ? userInfo.getEmail() : "anonymous");

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
