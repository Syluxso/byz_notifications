package com.nyberg.notifications.config;

import com.nyberg.notifications.tenant.OrganizationContext;
import com.nyberg.notifications.tenant.TenantContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JwtToUuidConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // User tokens: sub is a user UUID. Service tokens: sub is the clientId string.
        UUID userId = null;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ignored) {}

        // Populate thread-local contexts from JWT claims so services don't need them in request bodies
        String orgId = jwt.getClaimAsString("organization_id");
        if (orgId != null) {
            try { OrganizationContext.set(UUID.fromString(orgId)); }
            catch (IllegalArgumentException ignored) {}
        }

        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId != null) {
            try { TenantContext.set(UUID.fromString(tenantId)); }
            catch (IllegalArgumentException ignored) {}
        }

        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }
}
