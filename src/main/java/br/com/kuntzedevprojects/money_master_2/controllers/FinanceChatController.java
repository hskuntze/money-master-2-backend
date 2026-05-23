package br.com.kuntzedevprojects.money_master_2.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceChatRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceChatResponse;
import br.com.kuntzedevprojects.money_master_2.services.FinanceChatService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai/chat")
public class FinanceChatController {

    private final FinanceChatService financeChatService;

    public FinanceChatController(FinanceChatService financeChatService) {
        this.financeChatService = financeChatService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AI_CHAT_USE')")
    public ResponseEntity<FinanceChatResponse> chat(@Valid @RequestBody FinanceChatRequest request) {
        return ResponseEntity.ok(financeChatService.chat(request.message(), request.conversationId()));
    }
}
