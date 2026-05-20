package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @Size(max = 120, message = "O nome da conta deve ter no máximo 120 caracteres.")
        String name,
        AccountType type,
        BigDecimal initialBalance,
        Boolean active
) {
}
