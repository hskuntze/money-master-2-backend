package br.com.kuntzedevprojects.money_master_2.dtos.finance.payable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPayableSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPayableStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;

public record MonthlyPayableResponse(
        Long id,
        Long legacyPlanItemId,
        Long cycleId,
        String cycleName,
        String description,
        BigDecimal expectedAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        LocalDate dueDate,
        LocalDate paidOn,
        MonthlyPayableStatus status,
        MonthlyPayableSourceType sourceType,
        Long sourceId,
        boolean recurring,
        LocalDate recurrenceEndDate,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        AccountResponse account,
        CategoryResponse category,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static MonthlyPayableResponse from(MonthlyPlanItemResponse item) {
        return new MonthlyPayableResponse(
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
                mapStatus(item.status(), item.dueDate()),
                sourceType(item),
                item.id(),
                item.recurring(),
                item.recurrenceEndDate(),
                item.accountId(),
                item.accountName(),
                item.categoryId(),
                item.categoryName(),
                item.account(),
                item.category(),
                item.notes(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    private static MonthlyPayableStatus mapStatus(br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus status, LocalDate dueDate) {
        if ((status == br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus.PENDING
                || status == br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus.PARTIALLY_PAID)
                && dueDate != null
                && dueDate.isBefore(LocalDate.now())) {
            return MonthlyPayableStatus.OVERDUE;
        }
        return switch (status) {
            case PAID -> MonthlyPayableStatus.PAID;
            case PARTIALLY_PAID -> MonthlyPayableStatus.PARTIALLY_PAID;
            case CANCELED -> MonthlyPayableStatus.CANCELED;
            case PENDING -> MonthlyPayableStatus.PENDING;
        };
    }

    private static MonthlyPayableSourceType sourceType(MonthlyPlanItemResponse item) {
        if (item.aggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT
                || item.nature() == MonthlyPlanItemNature.CREDIT_CARD) {
            return MonthlyPayableSourceType.CREDIT_CARD_INVOICE;
        }
        if (item.nature() == MonthlyPlanItemNature.SAVINGS_JAR) {
            return MonthlyPayableSourceType.SAVINGS_JAR_CONTRIBUTION;
        }
        if (item.nature() == MonthlyPlanItemNature.INVESTMENT) {
            return MonthlyPayableSourceType.INVESTMENT_CONTRIBUTION;
        }
        if (item.generatedFromItemId() != null) {
            return MonthlyPayableSourceType.INSTALLMENT;
        }
        return MonthlyPayableSourceType.LEGACY_PLAN_ITEM;
    }
}
