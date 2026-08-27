package br.com.kuntzedevprojects.money_master_2.dtos.investment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;

public record InvestmentProductResponse(
        Long id,
        String name,
        String typeName,
        String institutionName,
        Long linkedAccountId,
        String linkedAccountName,
        String liquidity,
        boolean active,
        BigDecimal currentAmount,
        BigDecimal totalContributed,
        BigDecimal totalWithdrawn,
        BigDecimal totalYield,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static InvestmentProductResponse from(
            InvestmentProduct product,
            BigDecimal currentAmount,
            BigDecimal totalContributed,
            BigDecimal totalWithdrawn,
            BigDecimal totalYield
    ) {
        Account account = product.getLinkedAccount();
        return new InvestmentProductResponse(
                product.getId(),
                product.getName(),
                product.getTypeName(),
                product.getInstitutionName(),
                account == null ? null : account.getId(),
                account == null ? null : account.getName(),
                product.getLiquidity(),
                product.isActive(),
                money(currentAmount),
                money(totalContributed),
                money(totalWithdrawn),
                money(totalYield),
                product.getNotes(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
