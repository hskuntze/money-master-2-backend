package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinanceChatRequest(
        @NotBlank(message = "A mensagem é obrigatória.")
        @Size(max = 4000, message = "A mensagem deve ter no máximo 4000 caracteres.")
        String message,

        @Size(max = 80, message = "O identificador da conversa deve ter no máximo 80 caracteres.")
        String conversationId
) {
}
