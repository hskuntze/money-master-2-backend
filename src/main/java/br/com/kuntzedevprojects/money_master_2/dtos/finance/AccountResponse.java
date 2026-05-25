package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        boolean active,
        boolean internalDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                account.isActive(),
                account.isInternalDefault(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
