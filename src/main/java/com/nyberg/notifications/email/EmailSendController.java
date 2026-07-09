package com.nyberg.notifications.email;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/api/v1/admin/email")
@RequiredArgsConstructor
public class EmailSendController {

    private final EmailSendService sendService;

    @PostMapping("/send")
    public ResponseEntity<EmailSendResponse> send(@RequestBody EmailSendRequest request) {
        try {
            sendService.send(request);
            return ResponseEntity.ok(new EmailSendResponse(true, "Email sent successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new EmailSendResponse(false, e.getMessage()));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(new EmailSendResponse(false, "Provider error: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new EmailSendResponse(false, "Send failed: " + e.getMessage()));
        }
    }
}
