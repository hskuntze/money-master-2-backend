package br.com.kuntzedevprojects.money_master_2.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.admin.EmailSettingsResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.EmailSettingsUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.EmailTestRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.services.EmailService;
import br.com.kuntzedevprojects.money_master_2.services.EmailSettingsService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/email-settings")
@PreAuthorize("hasAuthority('EMAIL_SETTINGS_MANAGE')")
public class EmailSettingsController {

    private final EmailSettingsService emailSettingsService;
    private final EmailService emailService;

    public EmailSettingsController(EmailSettingsService emailSettingsService, EmailService emailService) {
        this.emailSettingsService = emailSettingsService;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<EmailSettingsResponse> getSettings() {
        return ResponseEntity.ok(emailSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<EmailSettingsResponse> update(@Valid @RequestBody EmailSettingsUpdateRequest request) {
        return ResponseEntity.ok(emailSettingsService.update(request));
    }

    @PostMapping("/test")
    public ResponseEntity<MessageResponse> test(@Valid @RequestBody EmailTestRequest request) {
        emailService.sendTestEmail(request.to());
        return ResponseEntity.ok(new MessageResponse("E-mail de teste enviado com sucesso."));
    }
}
