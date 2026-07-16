package br.com.kuntzedevprojects.money_master_2.dtos.finance.debt;

import jakarta.validation.constraints.Size;

public record DebtCancelRequest(
        @Size(max = 2000, message = "A justificativa deve ter no maximo 2000 caracteres.")
        String reason
) {
}
