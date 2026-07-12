package com.nyberg.notifications.notification.support;

import com.nyberg.notifications.tenant.OrganizationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Resolves the inbox recipient for in-app notification APIs.
 * <ul>
 *   <li>User / subject JWT: {@code sub} is a UUID → that is the recipient</li>
 *   <li>Service JWT ({@code client_credentials}): principal is null → require
 *       {@code X-Recipient-Id} (or {@code userId} query) and an org claim on the token</li>
 * </ul>
 */
@Component
public class RecipientResolver {

    public static final String RECIPIENT_HEADER = "X-Recipient-Id";

    public UUID requireRecipient(Authentication auth, String recipientHeader, UUID userIdParam) {
        if (auth != null && auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }

        UUID fromHeader = parseUuid(recipientHeader);
        UUID recipient = fromHeader != null ? fromHeader : userIdParam;
        if (recipient == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recipient required: use a subject/user JWT, or send " + RECIPIENT_HEADER + " (or userId) with a service token");
        }
        if (OrganizationContext.get() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Service token must include organization_id to act on behalf of a recipient");
        }
        return recipient;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, RECIPIENT_HEADER + " must be a valid UUID");
        }
    }
}
