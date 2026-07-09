package com.nyberg.notifications.email;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailProviderConfigRepository extends JpaRepository<EmailProviderConfig, UUID> {
    Optional<EmailProviderConfig> findByOrganizationId(UUID organizationId);
}
