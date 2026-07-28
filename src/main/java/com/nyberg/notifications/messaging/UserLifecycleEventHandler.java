package com.nyberg.notifications.messaging;

import com.nyberg.notifications.email.EmailSendRequest;
import com.nyberg.notifications.email.EmailSendService;
import com.nyberg.notifications.notification.dto.InAppNotificationRequest;
import com.nyberg.notifications.notification.service.NotificationService;
import com.nyberg.notifications.tenant.OrganizationContext;
import com.nyberg.notifications.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLifecycleEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final EmailSendService emailSendService;

    @Transactional
    public void handle(UserLifecycleEvent event) {
        if (event.eventId() == null || event.type() == null) {
            log.warn("Ignoring malformed user lifecycle event (missing eventId or type)");
            return;
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping already-processed eventId={} type={}", event.eventId(), event.type());
            return;
        }

        if (UserLifecycleEvent.TYPE_USER_REGISTERED.equals(event.type())) {
            handleUserRegistered(event);
        } else if (UserLifecycleEvent.TYPE_PASSWORD_RESET_REQUESTED.equals(event.type())) {
            handlePasswordResetRequested(event);
        } else {
            log.info("Ignoring unknown user lifecycle type={} eventId={}", event.type(), event.eventId());
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.eventId())
                .eventType(event.type())
                .build());
    }

    private void handleUserRegistered(UserLifecycleEvent event) {
        if (event.userId() == null || event.organizationId() == null) {
            throw new IllegalArgumentException("user.registered requires userId and organizationId");
        }

        String display = (event.displayName() != null && !event.displayName().isBlank())
                ? event.displayName().trim()
                : "there";

        OrganizationContext.set(event.organizationId());
        TenantContext.set(event.tenantId());
        try {
            notificationService.createInApp(new InAppNotificationRequest(
                    event.organizationId(),
                    event.userId(),
                    "Welcome",
                    "Welcome, " + display + "! Your account is ready.",
                    null,
                    Map.of(
                            "eventId", event.eventId().toString(),
                            "type", event.type()
                    ),
                    "normal",
                    "byz.iam.user"
            ));
        } finally {
            OrganizationContext.clear();
            TenantContext.clear();
        }

        sendWelcomeEmailBestEffort(event, display);
    }

    private void handlePasswordResetRequested(UserLifecycleEvent event) {
        if (event.userId() == null || event.organizationId() == null) {
            throw new IllegalArgumentException("user.password_reset_requested requires userId and organizationId");
        }
        if (event.email() == null || event.email().isBlank() || event.resetUrl() == null || event.resetUrl().isBlank()) {
            log.warn("Password reset email skipped — missing email or resetUrl eventId={}", event.eventId());
            return;
        }

        String display = (event.displayName() != null && !event.displayName().isBlank())
                ? event.displayName().trim()
                : "there";
        String safeUrl = escapeHtml(event.resetUrl());

        try {
            emailSendService.send(new EmailSendRequest(
                    event.organizationId(),
                    event.email(),
                    display,
                    "Reset your password",
                    "<p>Hi " + escapeHtml(display) + ",</p>"
                            + "<p>We received a request to reset your Byzantine password.</p>"
                            + "<p><a href=\"" + safeUrl + "\">Reset password</a></p>"
                            + "<p>This link expires in one hour. If you did not request a reset, you can ignore this email.</p>"
                            + "<p style=\"color:#888;font-size:12px;word-break:break-all;\">" + safeUrl + "</p>",
                    event.userId(),
                    event.tenantId(),
                    "byz.iam.password-reset",
                    "normal"
            ));
        } catch (Exception e) {
            log.warn("Password reset email skipped for userId={} eventId={}: {}",
                    event.userId(), event.eventId(), e.toString());
        }
    }

    private void sendWelcomeEmailBestEffort(UserLifecycleEvent event, String display) {
        if (event.email() == null || event.email().isBlank()) {
            return;
        }
        try {
            emailSendService.send(new EmailSendRequest(
                    event.organizationId(),
                    event.email(),
                    display,
                    "Welcome",
                    "<p>Welcome, " + escapeHtml(display) + "!</p><p>Your account is ready.</p>",
                    event.userId(),
                    event.tenantId(),
                    "byz.iam.user",
                    "normal"
            ));
        } catch (Exception e) {
            log.warn("Welcome email skipped for userId={} eventId={}: {}",
                    event.userId(), event.eventId(), e.toString());
        }
    }

    private static String escapeHtml(String raw) {
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
