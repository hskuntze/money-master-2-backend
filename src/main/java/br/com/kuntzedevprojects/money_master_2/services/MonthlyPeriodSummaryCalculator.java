package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

@Service
public class MonthlyPeriodSummaryCalculator {

    public MonthlyPeriodSummaryResponse calculate(
            FinancialPeriod period,
            List<MonthlyPlanItem> items,
            List<FinancialTransaction> transactions
    ) {
        List<MonthlyPlanItem> safeItems = items == null ? List.of() : items;
        List<FinancialTransaction> safeTransactions = transactions == null ? List.of() : transactions;

        BigDecimal plannedIncome = sumExpected(safeItems, TransactionType.INCOME);
        BigDecimal plannedExpense = sumExpected(safeItems, TransactionType.EXPENSE);
        BigDecimal paidIncome = sumActual(safeItems, TransactionType.INCOME);
        BigDecimal paidExpense = sumActual(safeItems, TransactionType.EXPENSE);
        BigDecimal realizedIncome = sumTransactions(safeTransactions, TransactionType.INCOME);
        BigDecimal realizedExpense = sumTransactions(safeTransactions, TransactionType.EXPENSE);
        BigDecimal pendingIncome = plannedIncome.subtract(paidIncome).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingExpense = plannedExpense.subtract(paidExpense).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal plannedAvailable = plannedIncome.subtract(plannedExpense).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unplannedIncome = sumUnplannedProjectedImpact(safeTransactions, safeItems, TransactionType.INCOME);
        BigDecimal unplannedExpense = sumUnplannedProjectedImpact(safeTransactions, safeItems, TransactionType.EXPENSE);
        BigDecimal projectedAvailable = plannedAvailable
                .add(unplannedIncome)
                .subtract(unplannedExpense)
                .setScale(2, RoundingMode.HALF_UP);
        long pendingItems = safeItems.stream()
                .filter(this::includedInMainTotals)
                .filter(item -> item.getStatus() == MonthlyPlanItemStatus.PENDING || item.getStatus() == MonthlyPlanItemStatus.PARTIALLY_PAID)
                .count();
        long paidItems = safeItems.stream()
                .filter(this::includedInMainTotals)
                .filter(item -> item.getStatus() == MonthlyPlanItemStatus.PAID)
                .count();

        return new MonthlyPeriodSummaryResponse(
                FinancialPeriodResponse.from(period),
                plannedIncome,
                plannedExpense,
                paidIncome,
                paidExpense,
                pendingIncome,
                pendingExpense,
                realizedIncome,
                realizedExpense,
                plannedAvailable,
                unplannedIncome,
                unplannedExpense,
                projectedAvailable,
                pendingItems,
                paidItems
        );
    }

    private BigDecimal sumTransactions(List<FinancialTransaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(FinancialTransaction::getAmount)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumUnplannedProjectedImpact(List<FinancialTransaction> transactions, List<MonthlyPlanItem> items, TransactionType type) {
        boolean hasCreditCardPlanItem = items.stream().anyMatch(this::isCreditCardPlanItem);
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .filter(transaction -> transaction.getMonthlyPlanItem() == null)
                .filter(transaction -> !shouldIgnoreCreditCardPurchaseForProjection(transaction, hasCreditCardPlanItem))
                .map(FinancialTransaction::getAmount)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean shouldIgnoreCreditCardPurchaseForProjection(FinancialTransaction transaction, boolean hasCreditCardPlanItem) {
        return hasCreditCardPlanItem && isCreditCardPurchase(transaction);
    }

    private boolean isCreditCardPurchase(FinancialTransaction transaction) {
        if (transaction.getType() != TransactionType.EXPENSE) {
            return false;
        }
        String categoryName = transaction.getCategory() == null ? "" : transaction.getCategory().getName();
        String accountType = transaction.getAccount() == null || transaction.getAccount().getType() == null
                ? ""
                : transaction.getAccount().getType().name();
        return containsCreditCardText(categoryName) || "CREDIT_CARD".equals(accountType);
    }

    private boolean isCreditCardPlanItem(MonthlyPlanItem item) {
        return item.getType() == TransactionType.EXPENSE
                && item.getStatus() != MonthlyPlanItemStatus.CANCELED
                && item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT;
    }

    private String normalizeComparable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private boolean containsCreditCardText(String value) {
        String normalized = normalizeComparable(value);
        return normalized.contains("cartao credito")
                || normalized.contains("cartao de credito")
                || normalized.contains("fatura cartao");
    }

    private BigDecimal sumExpected(List<MonthlyPlanItem> items, TransactionType type) {
        return items.stream()
                .filter(item -> item.getType() == type)
                .filter(item -> item.getStatus() != MonthlyPlanItemStatus.CANCELED)
                .filter(this::includedInMainTotals)
                .map(MonthlyPlanItem::getExpectedAmount)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumActual(List<MonthlyPlanItem> items, TransactionType type) {
        return items.stream()
                .filter(item -> item.getType() == type)
                .filter(item -> item.getStatus() != MonthlyPlanItemStatus.CANCELED)
                .filter(this::includedInMainTotals)
                .map(MonthlyPlanItem::getActualAmount)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean includedInMainTotals(MonthlyPlanItem item) {
        return item.getAggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
