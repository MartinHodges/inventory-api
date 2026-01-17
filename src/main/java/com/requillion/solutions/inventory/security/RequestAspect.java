package com.requillion.solutions.inventory.security;

import com.requillion.solutions.inventory.model.User;
import com.requillion.solutions.inventory.repository.UserRepository;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RequestAspect {

    private final UserRepository userRepository;

    @Around("execution(* com.requillion.solutions.inventory.controller.*.*(..)) && @within(org.springframework.web.bind.annotation.RestController)")
    public Object extractUserInfo(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestContext existingContext = UserContext.getContext();

        if (existingContext == null) {
            LoggerUtil.warn(log, "No request context found for %s", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        UserInfo userInfo = existingContext.getUserInfo();

        if (userInfo == null) {
            LoggerUtil.debug(log, "No user info in context for %s", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        UUID keycloakId = userInfo.getSubAsUUID();

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setKeycloakId(keycloakId);
                    newUser.setFirstName(userInfo.getGivenName() != null ? userInfo.getGivenName() : "Unknown");
                    newUser.setLastName(userInfo.getFamilyName() != null ? userInfo.getFamilyName() : "User");
                    newUser.setEmail(userInfo.getEmail());

                    User savedUser = userRepository.save(newUser);

                    LoggerUtil.info(log, "Created new user: %s (keycloakId: %s)", savedUser, keycloakId);

                    return savedUser;
                });

        existingContext.setUser(user);

        LoggerUtil.debug(log, "Executing %s for user %s", joinPoint.getSignature().getName(), user);

        return joinPoint.proceed();
    }
}
