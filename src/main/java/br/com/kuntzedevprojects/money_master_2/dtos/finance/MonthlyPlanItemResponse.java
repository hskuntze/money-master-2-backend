package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public record MonthlyPlanItemResponse(
        Long id,
        Long financialPeriodId,
        String financialPeriodName,
        TransactionType type,
        String description,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        LocalDate paidOn,
        MonthlyPlanItemStatus status,
        MonthlyPlanItemNature nature,
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
    public static MonthlyPlanItemResponse from(MonthlyPlanItem item) {
        Account account = item.getAccount();
        Category category = item.getCategory();
        BigDecimal remaining = item.getExpectedAmount().subtract(item.getActualAmount()).max(BigDecimal.ZERO);
        return new MonthlyPlanItemResponse(
                item.getId(),
                item.getFinancialPeriod().getId(),
                item.getFinancialPeriod().getName(),
                item.getType(),
                item.getDescription(),
                item.getExpectedAmount(),
                item.getActualAmount(),
                remaining,
                item.getDueDate(),
                item.getPaidOn(),
                item.getStatus(),
                item.getNature(),
                item.isRecurring(),
                item.getRecurrenceEndDate(),
                account == null ? null : account.getId(),
                account == null ? null : account.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                account == null ? null : AccountResponse.from(account),
                category == null ? null : CategoryResponse.from(category),
                item.getNotes(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
