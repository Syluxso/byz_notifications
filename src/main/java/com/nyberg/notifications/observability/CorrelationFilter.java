package com.nyberg.notifications.observability;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationFilter implements Filter {

    public static final String HEADER = "X-Correlation-ID";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String id = ((HttpServletRequest) req).getHeader(HEADER);
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        CURRENT.set(id);
        ((HttpServletResponse) res).setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            CURRENT.remove();
        }
    }

    public static String current() {
        String id = CURRENT.get();
        return id != null ? id : UUID.randomUUID().toString();
    }
}
