package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public record MonthlyPlanItemResponse(
        Long id,
        Long financialPeriodId,
        String financialPeriodName,
        Long parentItemId,
        String parentItemDescription,
        TransactionType type,
        String description,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        LocalDate paidOn,
        MonthlyPlanItemStatus status,
        MonthlyPlanItemNature nature,
        MonthlyPlanItemAggregationType aggregationType,
        MonthlyPlanItemSettlementOrigin settlementOrigin,
        boolean paidByParent,
        boolean includedInMainTotals,
        boolean recurring,
        LocalDate recurrenceEndDate,
        Long recurringTemplateId,
        Long generatedFromItemId,
        String recurrenceKey,
        boolean recurrenceModifiedManually,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        AccountResponse account,
        CategoryResponse category,
        BigDecimal childExpectedTotal,
        BigDecimal childActualTotal,
        BigDecimal childDifference,
        List<MonthlyPlanItemResponse> children,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static MonthlyPlanItemResponse from(MonthlyPlanItem item) {
        return from(item, List.of());
    }

    public static MonthlyPlanItemResponse from(MonthlyPlanItem item, List<MonthlyPlanItemResponse> children) {
        Account account = item.getAccount();
        Category category = item.getCategory();
        MonthlyPlanItem parent = item.getParentItem();
        BigDecimal expected = nullToZero(item.getExpectedAmount());
        BigDecimal actual = nullToZero(item.getActualAmount());
        BigDecimal remaining = expected.subtract(actual).max(BigDecimal.ZERO);
        List<MonthlyPlanItemResponse> safeChildren = children == null ? List.of() : children;
        BigDecimal childExpectedTotal = safeChildren.stream()
                .map(MonthlyPlanItemResponse::expectedAmount)
                .map(MonthlyPlanItemResponse::nullToZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal childActualTotal = safeChildren.stream()
                .map(MonthlyPlanItemResponse::actualAmount)
                .map(MonthlyPlanItemResponse::nullToZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal childDifference = expected.subtract(childExpectedTotal);
        MonthlyPlanItemAggregationType aggregationType = item.getAggregationType() == null
                ? MonthlyPlanItemAggregationType.NORMAL
                : item.getAggregationType();
        return new MonthlyPlanItemResponse(
                item.getId(),
                item.getFinancialPeriod().getId(),
                item.getFinancialPeriod().getName(),
                parent == null ? null : parent.getId(),
                parent == null ? null : parent.getDescription(),
                item.getType(),
                item.getDescription(),
                expected,
                actual,
                remaining,
                item.getDueDate(),
                item.getPaidOn(),
                item.getStatus(),
                item.getNature(),
                aggregationType,
                item.getSettlementOrigin() == null ? MonthlyPlanItemSettlementOrigin.DIRECT : item.getSettlementOrigin(),
                item.isPaidByParent(),
                aggregationType != MonthlyPlanItemAggregationType.GROUP_CHILD,
                item.isRecurring(),
                item.getRecurrenceEndDate(),
                item.getRecurringTemplateId(),
                item.getGeneratedFromItemId(),
                item.getRecurrenceKey(),
                item.isRecurrenceModifiedManually(),
                account == null ? null : account.getId(),
                account == null ? null : account.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                account == null ? null : AccountResponse.from(account),
                category == null ? null : CategoryResponse.from(category),
                childExpectedTotal,
                childActualTotal,
                childDifference,
                safeChildren,
                item.getNotes(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
