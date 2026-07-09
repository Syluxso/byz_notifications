CREATE TABLE notifications.email_provider_configs (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id       UUID         NOT NULL,
    provider              VARCHAR(50)  NOT NULL,
    credentials_encrypted TEXT         NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (organization_id)
);

CREATE INDEX idx_email_cfg_org ON notifications.email_provider_configs(organization_id);
