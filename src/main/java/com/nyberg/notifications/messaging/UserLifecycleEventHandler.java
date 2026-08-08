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
        } else if (UserLifecycleEvent.TYPE_DEVICE_REGISTERED.equals(event.type())) {
            handleDeviceRegistered(event);
        } else if (UserLifecycleEvent.TYPE_DEVICE_REVOKED.equals(event.type())) {
            handleDeviceRevoked(event);
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

        // In-app welcome (always attempted).
        OrganizationContext.set(event.organizationId());
        TenantContext.set(event.tenantId());
        try {
            notificationService.createInApp(new InAppNotificationRequest(
                    event.organizationId(),
                    event.userId(),
                    "Welcome to Byzantine",
                    "Welcome, " + display + "! Your account is ready. Explore the platform and complete your profile when you can.",
                    "/app/me",
                    Map.of(
                            "eventId", event.eventId().toString(),
                            "type", event.type()
                    ),
                    "normal",
                    "byz.iam.user"
            ));
            log.info("In-app welcome created userId={} orgId={}", event.userId(), event.organizationId());
        } catch (Exception e) {
            log.warn("In-app welcome failed userId={} eventId={}: {}",
                    event.userId(), event.eventId(), e.toString());
        } finally {
            OrganizationContext.clear();
            TenantContext.clear();
        }

        // Email welcome (needs org email provider in Admin, e.g. SendGrid).
        sendWelcomeEmailBestEffort(event, display);
    }

    private void handleDeviceRegistered(UserLifecycleEvent event) {
        if (event.userId() == null || event.organizationId() == null) {
            throw new IllegalArgumentException("device.registered requires userId and organizationId");
        }

        String label = (event.deviceLabel() != null && !event.deviceLabel().isBlank())
                ? event.deviceLabel().trim()
                : "A new device";
        String ipPart = (event.deviceIp() != null && !event.deviceIp().isBlank())
                ? " (IP " + event.deviceIp().trim() + ")"
                : "";
        String body = label + " was added to your account" + ipPart
                + ". If this wasn't you, review and revoke it under Security → Devices.";

        OrganizationContext.set(event.organizationId());
        TenantContext.set(event.tenantId());
        try {
            notificationService.createInApp(new InAppNotificationRequest(
                    event.organizationId(),
                    event.userId(),
                    "New device signed in",
                    body,
                    "/app/security/devices",
                    Map.of(
                            "eventId", event.eventId().toString(),
                            "type", event.type(),
                            "deviceId", event.deviceId() != null ? event.deviceId().toString() : ""
                    ),
                    "high",
                    "byz.iam.device"
            ));
            log.info("In-app new-device notice created userId={} deviceId={}",
                    event.userId(), event.deviceId());
        } catch (Exception e) {
            log.warn("In-app new-device notice failed userId={} eventId={}: {}",
                    event.userId(), event.eventId(), e.toString());
        } finally {
            OrganizationContext.clear();
            TenantContext.clear();
        }

        sendNewDeviceEmailBestEffort(event, label);
    }

    private void sendNewDeviceEmailBestEffort(UserLifecycleEvent event, String label) {
        if (event.email() == null || event.email().isBlank()) {
            log.warn("New-device email skipped — no email on eventId={}", event.eventId());
            return;
        }
        String display = (event.displayName() != null && !event.displayName().isBlank())
                ? event.displayName().trim()
                : "there";
        String safeLabel = escapeHtml(label);
        String safeIp = (event.deviceIp() != null && !event.deviceIp().isBlank())
                ? escapeHtml(event.deviceIp().trim())
                : null;
        String html = ""
                + "<div style=\"font-family:Helvetica,Arial,sans-serif;line-height:1.5;color:#151A20;\">"
                + "<p>Hi " + escapeHtml(display) + ",</p>"
                + "<p>A new device signed in to your <strong>Byzantine</strong> account.</p>"
                + "<p><strong>" + safeLabel + "</strong>"
                + (safeIp != null ? "<br/>IP: " + safeIp : "")
                + "</p>"
                + "<p>If this was you, you can ignore this message. If not, sign in and revoke the device under "
                + "<strong>Security → Devices</strong> (path <code>/app/security/devices</code>).</p>"
                + "<p style=\"margin-top:24px;color:#5C6874;font-size:13px;\">— The Byzantine team</p>"
                + "</div>";
        try {
            emailSendService.send(new EmailSendRequest(
                    event.organizationId(),
                    event.email(),
                    display,
                    "New device signed in to your account",
                    html,
                    event.userId(),
                    event.tenantId(),
                    "byz.iam.device",
                    "high"
            ));
            log.info("New-device email sent userId={} to={}", event.userId(), event.email());
        } catch (Exception e) {
            log.warn("New-device email skipped for userId={} eventId={} (configure org SendGrid in Admin): {}",
                    event.userId(), event.eventId(), e.toString());
        }
    }

    private void handleDeviceRevoked(UserLifecycleEvent event) {
        if (event.userId() == null || event.organizationId() == null) {
            throw new IllegalArgumentException("device.revoked requires userId and organizationId");
        }

        String label = (event.deviceLabel() != null && !event.deviceLabel().isBlank())
                ? event.deviceLabel().trim()
                : "A device";
        String ipPart = (event.deviceIp() != null && !event.deviceIp().isBlank())
                ? " (IP " + event.deviceIp().trim() + ")"
                : "";
        String body = label + " was revoked and signed out" + ipPart
                + ". If you did not do this, review your remaining devices under Security → Devices.";

        OrganizationContext.set(event.organizationId());
        TenantContext.set(event.tenantId());
        try {
            notificationService.createInApp(new InAppNotificationRequest(
                    event.organizationId(),
                    event.userId(),
                    "Device revoked",
                    body,
                    "/app/security/devices",
                    Map.of(
                            "eventId", event.eventId().toString(),
                            "type", event.type(),
                            "deviceId", event.deviceId() != null ? event.deviceId().toString() : ""
                    ),
                    "high",
                    "byz.iam.device"
            ));
            log.info("In-app device-revoked notice created userId={} deviceId={}",
                    event.userId(), event.deviceId());
        } catch (Exception e) {
            log.warn("In-app device-revoked notice failed userId={} eventId={}: {}",
                    event.userId(), event.eventId(), e.toString());
        } finally {
            OrganizationContext.clear();
            TenantContext.clear();
        }

        sendDeviceRevokedEmailBestEffort(event, label);
    }

    private void sendDeviceRevokedEmailBestEffort(UserLifecycleEvent event, String label) {
        if (event.email() == null || event.email().isBlank()) {
            log.warn("Device-revoked email skipped — no email on eventId={}", event.eventId());
            return;
        }
        String display = (event.displayName() != null && !event.displayName().isBlank())
                ? event.displayName().trim()
                : "there";
        String safeLabel = escapeHtml(label);
        String safeIp = (event.deviceIp() != null && !event.deviceIp().isBlank())
                ? escapeHtml(event.deviceIp().trim())
                : null;
        String html = ""
                + "<div style=\"font-family:Helvetica,Arial,sans-serif;line-height:1.5;color:#151A20;\">"
                + "<p>Hi " + escapeHtml(display) + ",</p>"
                + "<p>A device was revoked on your <strong>Byzantine</strong> account and signed out.</p>"
                + "<p><strong>" + safeLabel + "</strong>"
                + (safeIp != null ? "<br/>IP: " + safeIp : "")
                + "</p>"
                + "<p>If you did this, no further action is needed. If not, sign in and review your devices under "
                + "<strong>Security → Devices</strong> (path <code>/app/security/devices</code>).</p>"
                + "<p style=\"margin-top:24px;color:#5C6874;font-size:13px;\">— The Byzantine team</p>"
                + "</div>";
        try {
            emailSendService.send(new EmailSendRequest(
                    event.organizationId(),
                    event.email(),
                    display,
                    "A device was revoked on your account",
                    html,
                    event.userId(),
                    event.tenantId(),
                    "byz.iam.device",
                    "high"
            ));
            log.info("Device-revoked email sent userId={} to={}", event.userId(), event.email());
        } catch (Exception e) {
            log.warn("Device-revoked email skipped for userId={} eventId={} (configure org SendGrid in Admin): {}",
                    event.userId(), event.eventId(), e.toString());
        }
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
            log.warn("Welcome email skipped — no email on eventId={}", event.eventId());
            return;
        }
        String safeName = escapeHtml(display);
        String html = ""
                + "<div style=\"font-family:Helvetica,Arial,sans-serif;line-height:1.5;color:#151A20;\">"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Welcome to <strong>Byzantine</strong>. Your account is ready.</p>"
                + "<p>Sign in anytime to access your workspace and stay on top of what matters.</p>"
                + "<p style=\"margin-top:24px;color:#5C6874;font-size:13px;\">— The Byzantine team</p>"
                + "</div>";
        try {
            emailSendService.send(new EmailSendRequest(
                    event.organizationId(),
                    event.email(),
                    display,
                    "Welcome to Byzantine",
                    html,
                    event.userId(),
                    event.tenantId(),
                    "byz.iam.welcome",
                    "normal"
            ));
            log.info("Welcome email sent userId={} to={}", event.userId(), event.email());
        } catch (Exception e) {
            log.warn("Welcome email skipped for userId={} eventId={} (configure org SendGrid in Admin): {}",
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
