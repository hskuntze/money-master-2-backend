package br.com.kuntzedevprojects.money_master_2.dtos.finance.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyIncomePlanStatus;

public record MonthlyIncomePlanResponse(
        Long id,
        Long legacyPlanItemId,
        Long cycleId,
        String cycleName,
        String description,
        BigDecimal expectedAmount,
        BigDecimal receivedAmount,
        BigDecimal pendingAmount,
        LocalDate expectedDate,
        LocalDate receivedOn,
        MonthlyIncomePlanStatus status,
        boolean recurring,
        LocalDate recurrenceEndDate,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        AccountResponse account,
        CategoryResponse category,
        String source,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static MonthlyIncomePlanResponse from(MonthlyPlanItemResponse item) {
        return new MonthlyIncomePlanResponse(
                item.id(),
                item.id(),
                item.financialPeriodId(),
                item.financialPeriodName(),
                item.description(),
                item.expectedAmount(),
                item.actualAmount(),
                item.remainingAmount(),
                item.dueDate(),
                item.paidOn(),
                mapStatus(item.status()),
                item.recurring(),
                item.recurrenceEndDate(),
                item.accountId(),
                item.accountName(),
                item.categoryId(),
                item.categoryName(),
                item.account(),
                item.category(),
                "LEGACY_PLAN_ITEM",
                item.notes(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    private static MonthlyIncomePlanStatus mapStatus(br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus status) {
        return switch (status) {
            case PAID -> MonthlyIncomePlanStatus.RECEIVED;
            case PARTIALLY_PAID -> MonthlyIncomePlanStatus.PARTIALLY_RECEIVED;
            case CANCELED -> MonthlyIncomePlanStatus.CANCELED;
            case PENDING -> MonthlyIncomePlanStatus.EXPECTED;
        };
    }
}
