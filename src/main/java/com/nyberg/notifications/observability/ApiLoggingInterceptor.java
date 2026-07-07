package com.nyberg.notifications.observability;

import com.nyberg.notifications.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiLoggingInterceptor implements HandlerInterceptor {

    private final LogClient logClient;
    private static final String START_ATTR = "obs.start";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        req.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        Long start = (Long) req.getAttribute(START_ATTR);
        long duration = start != null ? System.currentTimeMillis() - start : 0;
        int status = res.getStatus();

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

        String level = status >= 500 ? "ERROR" : status >= 400 ? "WARN" : "INFO";
        String message = req.getMethod() + " " + req.getRequestURI() + " → " + status;

        Map<String, Object> details = new HashMap<>();
        details.put("method", req.getMethod());
        details.put("path", req.getRequestURI());
        details.put("status", status);
        if (ex != null) details.put("error", ex.getMessage());

        logClient.send(ObsLogEntry.of(level, "api_call", message, tenantId, userId, duration, details));

        if (duration > 500) {
            logClient.warn("slow_operation",
                    req.getMethod() + " " + req.getRequestURI() + " took " + duration + "ms",
                    tenantId, userId, duration, Map.of("path", req.getRequestURI(), "threshold_ms", 500));
        }
    }
}
