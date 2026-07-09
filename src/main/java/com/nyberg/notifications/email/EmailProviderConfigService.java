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
        String encrypted = encrypt(req.apiKey());

        EmailProviderConfig existing = repo.findByOrganizationId(req.organizationId()).orElse(null);
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

    private String encrypt(String apiKey) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("apiKey", apiKey));
            return encryption.encrypt(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }

    private EmailProviderConfigResponse toResponse(EmailProviderConfig cfg) {
        String hint = keyHint(cfg.getCredentialsEncrypted());
        return new EmailProviderConfigResponse(
                cfg.getId(),
                cfg.getOrganizationId(),
                cfg.getProvider(),
                hint,
                cfg.isActive(),
                cfg.getCreatedAt(),
                cfg.getUpdatedAt()
        );
    }

    private String keyHint(String encrypted) {
        try {
            String json = encryption.decrypt(encrypted);
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            String apiKey = (String) map.get("apiKey");
            if (apiKey == null || apiKey.length() < 4) return "****";
            return "..." + apiKey.substring(apiKey.length() - 4);
        } catch (Exception e) {
            return "****";
        }
    }
}
