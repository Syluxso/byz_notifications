package com.nyberg.notifications.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String header = request.getHeader("X-Tenant-ID");
        String path = request.getRequestURI();

        // Actuator endpoints and health checks don't need tenant context
        if (path.startsWith("/actuator")) {
            chain.doFilter(req, res);
            return;
        }

        if (header != null && !header.isBlank()) {
            try {
                TenantContext.set(UUID.fromString(header));
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-ID must be a valid UUID");
                return;
            }
        }

        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
