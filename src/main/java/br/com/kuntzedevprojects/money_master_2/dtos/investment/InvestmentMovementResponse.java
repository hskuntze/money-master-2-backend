package br.com.kuntzedevprojects.money_master_2.dtos.investment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.InvestmentMovement;
import br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;

public record InvestmentMovementResponse(
        Long id,
        Long investmentProductId,
        InvestmentMovementType type,
        BigDecimal amount,
        LocalDate occurredOn,
        String description,
        TransactionSource source,
        String notes,
        Instant createdAt
) {
    public static InvestmentMovementResponse from(InvestmentMovement movement) {
        return new InvestmentMovementResponse(
                movement.getId(),
                movement.getInvestmentProduct().getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getOccurredOn(),
                movement.getDescription(),
                movement.getSource(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }
}
