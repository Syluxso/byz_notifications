package com.nyberg.notifications.email;

import java.util.UUID;

public record EmailSendRequest(
        UUID organizationId,
        String toEmail,
        String toName,
        String subject,
        String htmlBody,
        UUID userId,    // optional — byz user ID of recipient if known
        UUID tenantId,  // optional — tenant context if known
        String source,  // optional — caller identifier
        String priority // optional — defaults to "normal"
) {}
