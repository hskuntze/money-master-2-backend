package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.SavingsJarMovement;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;

public record SavingsJarMovementResponse(
        Long id,
        Long savingsJarId,
        SavingsJarMovementType type,
        BigDecimal amount,
        LocalDate occurredOn,
        String description,
        TransactionSource source,
        BigDecimal baseAmount,
        BigDecimal rateApplied,
        String rateReference,
        String notes,
        Instant createdAt
) {
    public static SavingsJarMovementResponse from(SavingsJarMovement movement) {
        return new SavingsJarMovementResponse(
                movement.getId(),
                movement.getSavingsJar().getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getOccurredOn(),
                movement.getDescription(),
                movement.getSource(),
                movement.getBaseAmount(),
                movement.getRateApplied(),
                movement.getRateReference(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }
}
