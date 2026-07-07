CREATE TABLE notifications.notifications (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID,
    user_id     UUID         NOT NULL,
    channel     VARCHAR(50)  NOT NULL DEFAULT 'in_app',
    status      VARCHAR(30)  NOT NULL DEFAULT 'unread',
    title       TEXT         NOT NULL,
    message     TEXT         NOT NULL,
    link        TEXT,
    data        JSONB,
    source      VARCHAR(100),
    priority    VARCHAR(20)  NOT NULL DEFAULT 'normal',
    read_at     TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_user_status ON notifications.notifications(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_notif_tenant      ON notifications.notifications(tenant_id);
CREATE INDEX idx_notif_created     ON notifications.notifications(created_at DESC);
CREATE INDEX idx_notif_source      ON notifications.notifications(source);
