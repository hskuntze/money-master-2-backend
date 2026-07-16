package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiPrivacySettingsRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiPrivacySettingsResponse;
import br.com.kuntzedevprojects.money_master_2.services.AiPrivacySettingsService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai/privacy-settings")
public class AiPrivacySettingsController {

    private final AiPrivacySettingsService service;

    public AiPrivacySettingsController(AiPrivacySettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AI_CHAT_USE')")
    public ResponseEntity<AiPrivacySettingsResponse> get(Principal principal) {
        return ResponseEntity.ok(service.get(principal.getName()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('AI_CHAT_USE')")
    public ResponseEntity<AiPrivacySettingsResponse> update(
            Principal principal,
            @Valid @RequestBody AiPrivacySettingsRequest request
    ) {
        return ResponseEntity.ok(service.update(principal.getName(), request));
    }

    @DeleteMapping("/history")
    @PreAuthorize("hasAuthority('AI_CHAT_USE')")
    public ResponseEntity<Void> deleteHistory(Principal principal) {
        service.deleteChatHistory(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
