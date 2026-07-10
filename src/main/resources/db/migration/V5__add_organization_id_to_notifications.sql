ALTER TABLE notifications.notifications
    ADD COLUMN organization_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

ALTER TABLE notifications.notifications
    ALTER COLUMN organization_id DROP DEFAULT;

CREATE INDEX idx_notif_organization_id ON notifications.notifications(organization_id);
