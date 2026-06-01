package com.digitaltwin.central.controller;

import com.digitaltwin.central.model.WebhookSubscriber;
import com.digitaltwin.central.repository.NotificationAttemptRepository;
import com.digitaltwin.central.repository.WebhookSubscriberRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Webhooks", description = "Manage webhook subscribers and delivery attempts. Use to register callback URLs to receive notifications about events and alerts.")
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookSubscriberRepository repo;
    private final NotificationAttemptRepository attemptRepository;

    public WebhookController(WebhookSubscriberRepository repo, NotificationAttemptRepository attemptRepository) {
        this.repo = repo;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping
    @Operation(summary = "List webhooks", description = "Return all registered webhook subscribers. Use to audit or manage subscriber endpoints.")
    public List<WebhookSubscriber> list() {
        return repo.findAll();
    }

    public static class CreateReq {
        @NotBlank
        public String url;
        public String secret;
    }

    @PostMapping
    @Operation(summary = "Create webhook subscriber", description = "Register a new webhook URL to receive notifications. Provide an optional secret for HMAC signing of payloads.")
    public ResponseEntity<?> create(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Webhook creation payload") @RequestBody CreateReq req) {
        WebhookSubscriber s = new WebhookSubscriber();
        s.setUrl(req.url);
        s.setSecret(req.secret);
        s.setActive(true);
        WebhookSubscriber saved = repo.save(s);
        return ResponseEntity.created(URI.create("/api/webhooks/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete webhook subscriber", description = "Remove a webhook subscriber and all associated notification attempts. Use when a subscriber should no longer receive notifications.")
    @Transactional
    public ResponseEntity<?> delete(@Parameter(description = "ID of webhook subscriber") @PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        attemptRepository.deleteBySubscriberId(id);
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
