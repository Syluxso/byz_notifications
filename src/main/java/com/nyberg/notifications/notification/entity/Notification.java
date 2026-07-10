package com.nyberg.notifications.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notifications")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "channel", nullable = false, length = 50)
    @Builder.Default
    private String channel = "in_app";

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "unread";

    @Column(name = "delivery_mode", nullable = false, length = 20)
    @Builder.Default
    private String deliveryMode = "direct";

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "link")
    private String link;

    @Column(name = "data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String data;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private String priority = "normal";

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public void markRead() {
        this.status = "read";
        this.readAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = "deleted";
    }
}
