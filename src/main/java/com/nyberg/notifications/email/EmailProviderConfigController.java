package com.nyberg.notifications.email;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/email-config")
@RequiredArgsConstructor
public class EmailProviderConfigController {

    private final EmailProviderConfigService service;

    @GetMapping
    public List<EmailProviderConfigResponse> list() {
        return service.listAll();
    }

    @PostMapping
    public EmailProviderConfigResponse upsert(@RequestBody EmailProviderConfigRequest req) {
        return service.upsert(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
