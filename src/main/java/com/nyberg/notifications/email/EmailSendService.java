package com.nyberg.notifications.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailSendService {

    private final EmailProviderConfigRepository configRepo;
    private final CredentialEncryptionService encryption;
    private final ObjectMapper objectMapper;
    private final List<EmailProvider> providers;

    public void send(EmailSendRequest request) {
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

        Map<String, String> credentials = decryptCredentials(cfg.getCredentialsEncrypted());
        provider.send(credentials, request);
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
