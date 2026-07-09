package com.nyberg.notifications.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailProviderConfigService {

    private final EmailProviderConfigRepository repo;
    private final CredentialEncryptionService encryption;
    private final ObjectMapper objectMapper;

    public List<EmailProviderConfigResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmailProviderConfigResponse upsert(EmailProviderConfigRequest req) {
        EmailProviderConfig existing = repo.findByOrganizationId(req.organizationId()).orElse(null);

        Map<String, String> merged = new java.util.HashMap<>(req.credentials());

        // If apiKey is absent or blank, preserve the existing one
        if ((merged.get("apiKey") == null || merged.get("apiKey").isBlank()) && existing != null) {
            Map<String, String> stored = decryptCredentials(existing.getCredentialsEncrypted());
            merged.put("apiKey", stored.get("apiKey"));
        }

        String encrypted = encrypt(merged);

        if (existing != null) {
            existing.setProvider(req.provider());
            existing.setCredentialsEncrypted(encrypted);
            existing.setActive(true);
            return toResponse(repo.save(existing));
        }

        EmailProviderConfig cfg = EmailProviderConfig.builder()
                .organizationId(req.organizationId())
                .provider(req.provider())
                .credentialsEncrypted(encrypted)
                .build();
        return toResponse(repo.save(cfg));
    }

    @Transactional
    public void delete(UUID id) {
        repo.deleteById(id);
    }

    private String encrypt(Map<String, String> credentials) {
        try {
            String json = objectMapper.writeValueAsString(credentials);
            return encryption.encrypt(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }

    private EmailProviderConfigResponse toResponse(EmailProviderConfig cfg) {
        Map<String, String> creds = decryptCredentials(cfg.getCredentialsEncrypted());
        String apiKey = creds.getOrDefault("apiKey", "");
        String hint = apiKey.length() >= 4 ? "..." + apiKey.substring(apiKey.length() - 4) : "****";
        return new EmailProviderConfigResponse(
                cfg.getId(),
                cfg.getOrganizationId(),
                cfg.getProvider(),
                hint,
                creds.getOrDefault("fromEmail", ""),
                creds.getOrDefault("fromName", ""),
                cfg.isActive(),
                cfg.getCreatedAt(),
                cfg.getUpdatedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> decryptCredentials(String encrypted) {
        try {
            String json = encryption.decrypt(encrypted);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
