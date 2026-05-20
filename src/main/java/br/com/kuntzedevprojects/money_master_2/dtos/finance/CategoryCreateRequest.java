package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 120, message = "O nome da categoria deve ter no máximo 120 caracteres.")
        String name,

        @NotNull(message = "O tipo da categoria é obrigatório.")
        TransactionType type,

        @Size(max = 50, message = "O ícone deve ter no máximo 50 caracteres.")
        String icon,

        @Size(max = 20, message = "A cor deve ter no máximo 20 caracteres.")
        String color,

        Boolean active
) {
}
