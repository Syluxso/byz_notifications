package com.nyberg.notifications.observability;

import com.nyberg.notifications.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LogClient logClient;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        if (ex.getStatusCode().value() >= 500) {
            sendErrorLog(ex, req);
        }
        String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        sendErrorLog(ex, req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal server error"));
    }

    private void sendErrorLog(Exception ex, HttpServletRequest req) {
        String tenantId = null;
        String userId = null;
        try {
            UUID tenantUuid = TenantContext.get();
            if (tenantUuid != null) tenantId = tenantUuid.toString();
        } catch (Exception ignored) {}
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UUID id) userId = id.toString();
        } catch (Exception ignored) {}

        String stackTrace = Arrays.stream(ex.getStackTrace())
                .limit(8)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

        Map<String, Object> details = new HashMap<>();
        details.put("method", req.getMethod());
        details.put("path", req.getRequestURI());
        details.put("exception", ex.getClass().getName());
        details.put("stack_trace", stackTrace);

        logClient.error("error",
                ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "(no message)"),
                tenantId, userId, null, details);
    }
}
