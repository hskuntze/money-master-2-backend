package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 120, message = "O nome da conta deve ter no máximo 120 caracteres.")
        String name,

        @NotNull(message = "O tipo da conta é obrigatório.")
        AccountType type,

        BigDecimal initialBalance,

        Boolean active
) {
}
