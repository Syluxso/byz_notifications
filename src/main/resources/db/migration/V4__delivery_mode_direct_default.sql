ALTER TABLE notifications.notifications
    ALTER COLUMN delivery_mode SET DEFAULT 'direct';

UPDATE notifications.notifications
    SET delivery_mode = 'direct'
    WHERE channel = 'in_app';
