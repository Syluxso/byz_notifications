package com.nyberg.notifications.email;

import java.util.UUID;

public record EmailSendRequest(
        UUID organizationId,
        String toEmail,
        String toName,
        String subject,
        String htmlBody
) {}
