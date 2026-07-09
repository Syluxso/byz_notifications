package com.nyberg.notifications.email;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class SendGridEmailProvider implements EmailProvider {

    private static final String API_URL = "https://api.sendgrid.com/v3/mail/send";

    private final RestClient restClient;

    public SendGridEmailProvider(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public String providerId() {
        return "sendgrid";
    }

    @Override
    public void send(Map<String, String> credentials, EmailSendRequest req) {
        String apiKey   = credentials.get("apiKey");
        String fromEmail = credentials.get("fromEmail");
        String fromName  = credentials.getOrDefault("fromName", "");

        Map<String, Object> body = Map.of(
            "personalizations", List.of(Map.of(
                "to", List.of(toAddress(req.toEmail(), req.toName()))
            )),
            "from",    toAddress(fromEmail, fromName),
            "subject", req.subject(),
            "content", List.of(Map.of("type", "text/html", "value", req.htmlBody()))
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
