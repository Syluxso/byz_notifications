package com.nyberg.notifications.email;

import java.util.Map;

public interface EmailProvider {
    void send(Map<String, String> credentials, EmailSendRequest request);
    String providerId();
}
