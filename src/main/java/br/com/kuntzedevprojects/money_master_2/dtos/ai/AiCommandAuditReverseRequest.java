package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import jakarta.validation.constraints.Size;

public record AiCommandAuditReverseRequest(
        Boolean deleteLinkedTransaction,
        @Size(max = 2000, message = "As observacoes devem ter no maximo 2000 caracteres.")
        String notes
) {
}
