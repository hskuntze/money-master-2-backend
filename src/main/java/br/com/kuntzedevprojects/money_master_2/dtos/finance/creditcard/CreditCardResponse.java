package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;

public record CreditCardResponse(
        Long id,
        Long accountId,
        String accountName,
        String name,
        String brand,
        BigDecimal limitAmount,
        Integer closingDay,
        Integer dueDay,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CreditCardResponse from(CreditCard card) {
        return new CreditCardResponse(
                card.getId(),
                card.getAccount() == null ? null : card.getAccount().getId(),
                card.getAccount() == null ? null : card.getAccount().getName(),
                card.getName(),
                card.getBrand(),
                card.getLimitAmount(),
                card.getClosingDay(),
                card.getDueDay(),
                card.isActive(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
