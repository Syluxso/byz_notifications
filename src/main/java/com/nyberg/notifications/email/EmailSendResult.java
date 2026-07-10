package com.nyberg.notifications.email;

public record EmailSendResult(boolean success, String message, String notificationId) {}
