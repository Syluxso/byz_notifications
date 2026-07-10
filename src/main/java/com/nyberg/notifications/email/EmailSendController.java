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
    public ResponseEntity<EmailSendResult> send(@RequestBody EmailSendRequest request) {
        try {
            EmailSendResult result = sendService.send(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new EmailSendResult(false, e.getMessage(), null));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(new EmailSendResult(false, "Provider error: " + e.getResponseBodyAsString(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new EmailSendResult(false, "Send failed: " + e.getMessage(), null));
        }
    }
}
