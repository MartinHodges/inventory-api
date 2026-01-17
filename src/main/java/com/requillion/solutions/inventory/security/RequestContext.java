package com.requillion.solutions.inventory.security;

import com.requillion.solutions.inventory.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RequestContext {

    private final UUID requestId;
    private final UserInfo userInfo;
    private User user;

    public RequestContext(UUID requestId, UserInfo userInfo) {
        this.requestId = requestId;
        this.userInfo = userInfo;
        this.user = null;
    }

    public boolean isAuthenticated() {
        return userInfo != null && user != null;
    }
}
