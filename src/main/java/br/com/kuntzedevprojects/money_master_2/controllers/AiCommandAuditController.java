package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditResponse;
import br.com.kuntzedevprojects.money_master_2.services.AiCommandAuditService;

@RestController
@RequestMapping("/ai/command-audits")
public class AiCommandAuditController {

    private final AiCommandAuditService service;

    public AiCommandAuditController(AiCommandAuditService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AI_CHAT_USE')")
    public ResponseEntity<List<AiCommandAuditResponse>> list(
            Principal principal,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.listRecent(principal.getName(), limit));
    }
}
