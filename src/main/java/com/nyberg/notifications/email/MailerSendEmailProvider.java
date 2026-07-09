package com.nyberg.notifications.email;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class MailerSendEmailProvider implements EmailProvider {

    private static final String API_URL = "https://api.mailersend.com/v1/email";

    private final RestClient restClient;

    public MailerSendEmailProvider(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public String providerId() {
        return "mailersend";
    }

    @Override
    public void send(Map<String, String> credentials, EmailSendRequest req) {
        String apiKey    = credentials.get("apiKey");
        String fromEmail = credentials.get("fromEmail");
        String fromName  = credentials.getOrDefault("fromName", "");

        Map<String, Object> body = Map.of(
            "from",    toAddress(fromEmail, fromName),
            "to",      List.of(toAddress(req.toEmail(), req.toName())),
            "subject", req.subject(),
            "html",    req.htmlBody()
        );

        restClient.post()
            .uri(API_URL)
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    private Map<String, String> toAddress(String email, String name) {
        if (name != null && !name.isBlank()) {
            return Map.of("email", email, "name", name);
        }
        return Map.of("email", email);
    }
}
