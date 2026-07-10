ALTER TABLE notifications.notifications
    ADD COLUMN delivery_mode VARCHAR(20) NOT NULL DEFAULT 'queued';

ALTER TABLE notifications.notifications
    ALTER COLUMN user_id DROP NOT NULL;

CREATE INDEX idx_notif_delivery_mode ON notifications.notifications(delivery_mode);
