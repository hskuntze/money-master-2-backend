package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public record FinancialTransactionResponse(
        Long id,
        TransactionType type,
        String description,
        BigDecimal amount,
        LocalDate occurredOn,
        TransactionSource source,
        AccountResponse account,
        CategoryResponse category,
        Long financialPeriodId,
        String financialPeriodName,
        Long monthlyPlanItemId,
        String monthlyPlanItemStatus,
        String aiRawMessage,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static FinancialTransactionResponse from(FinancialTransaction transaction) {
        Category category = transaction.getCategory();
        FinancialPeriod period = transaction.getFinancialPeriod();
        MonthlyPlanItem planItem = transaction.getMonthlyPlanItem();
        return new FinancialTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getOccurredOn(),
                transaction.getSource(),
                AccountResponse.from(transaction.getAccount()),
                category == null ? null : CategoryResponse.from(category),
                period == null ? null : period.getId(),
                period == null ? null : period.getName(),
                planItem == null ? null : planItem.getId(),
                planItem == null ? null : planItem.getStatus().name(),
                transaction.getAiRawMessage(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
