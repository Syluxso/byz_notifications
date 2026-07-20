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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    /**
     * Browser closed / navigated away from an SSE stream. Not an application failure —
     * and we must not try to write a JSON body onto an already-open text/event-stream response.
     */
    @ExceptionHandler({AsyncRequestNotUsableException.class})
    public ResponseEntity<Void> handleClientGone(AsyncRequestNotUsableException ex, HttpServletRequest req) {
        log.debug("Client disconnected {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest req) {
        if (isClientDisconnect(ex)) {
            log.debug("Client disconnected {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
            return ResponseEntity.noContent().build();
        }
        log.error("Unhandled exception {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        sendErrorLog(ex, req);
        // SSE responses cannot accept a JSON error body once Content-Type is text/event-stream.
        if (isEventStream(req)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal server error"));
    }

    private static boolean isEventStream(HttpServletRequest req) {
        String path = req.getRequestURI();
        return path != null && path.contains("/stream");
    }

    private static boolean isClientDisconnect(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof AsyncRequestNotUsableException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("ClientAbortException") || name.contains("EofException")) {
                return true;
            }
            if (cur instanceof IOException) {
                String msg = cur.getMessage();
                if (msg != null) {
                    String m = msg.toLowerCase();
                    if (m.contains("broken pipe")
                            || m.contains("connection reset")
                            || m.contains("disconnected client")
                            || m.contains("async request not usable")) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
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
