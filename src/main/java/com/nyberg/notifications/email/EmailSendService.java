package com.nyberg.notifications.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyberg.notifications.notification.entity.Notification;
import com.nyberg.notifications.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailSendService {

    private final EmailProviderConfigRepository configRepo;
    private final CredentialEncryptionService encryption;
    private final NotificationRepository notificationRepo;
    private final ObjectMapper objectMapper;
    private final List<EmailProvider> providers;

    public EmailSendResult send(EmailSendRequest request) {
        EmailProviderConfig cfg = configRepo.findByOrganizationId(request.organizationId())
            .orElseThrow(() -> new IllegalStateException(
                "No email config found for organization " + request.organizationId()));

        if (!cfg.isActive()) {
            throw new IllegalStateException("Email config for this organization is inactive");
        }

        EmailProvider provider = providers.stream()
            .filter(p -> p.providerId().equals(cfg.getProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No driver registered for provider: " + cfg.getProvider()));

        // Create the notification record as queued/direct
        Notification notification = Notification.builder()
            .userId(request.userId())
            .tenantId(request.tenantId())
            .channel("email")
            .status("queued")
            .deliveryMode("direct")
            .title(request.subject())
            .message("Email to: " + request.toEmail())
            .source(request.source() != null ? request.source() : "admin")
            .priority(request.priority() != null ? request.priority() : "normal")
            .data(buildData(request, cfg.getProvider()))
            .build();

        notificationRepo.save(notification);

        // Attempt delivery
        try {
            Map<String, String> credentials = decryptCredentials(cfg.getCredentialsEncrypted());
            provider.send(credentials, request);
            notification.setStatus("sent");
            notificationRepo.save(notification);
            return new EmailSendResult(true, "Email sent successfully", notification.getId().toString());
        } catch (Exception e) {
            notification.setStatus("failed");
            notification.setData(buildDataWithError(request, cfg.getProvider(), e.getMessage()));
            notificationRepo.save(notification);
            throw e;
        }
    }

    private String buildData(EmailSendRequest req, String provider) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "toEmail",  req.toEmail(),
                "toName",   req.toName() != null ? req.toName() : "",
                "subject",  req.subject(),
                "provider", provider
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildDataWithError(EmailSendRequest req, String provider, String error) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "toEmail",  req.toEmail(),
                "toName",   req.toName() != null ? req.toName() : "",
                "subject",  req.subject(),
                "provider", provider,
                "error",    error != null ? error : "unknown"
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> decryptCredentials(String encrypted) {
        try {
            String json = encryption.decrypt(encrypted);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt email credentials", e);
        }
    }
}
