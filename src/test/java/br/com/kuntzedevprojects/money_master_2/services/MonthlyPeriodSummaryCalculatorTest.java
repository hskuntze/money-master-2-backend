package br.com.kuntzedevprojects.money_master_2.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

class MonthlyPeriodSummaryCalculatorTest {

    private final MonthlyPeriodSummaryCalculator calculator = new MonthlyPeriodSummaryCalculator();
    private final FinancialPeriod period = period();

    @Test
    void shouldCalculateEmptyCycle() {
        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(), List.of());

        assertAll(
                () -> assertMoney("0.00", summary.plannedIncomeTotal()),
                () -> assertMoney("0.00", summary.plannedExpenseTotal()),
                () -> assertMoney("0.00", summary.projectedAvailableAmount()),
                () -> assertEquals(0, summary.pendingItems()),
                () -> assertEquals(0, summary.paidItems())
        );
    }

    @Test
    void shouldCalculatePlannedIncomeExpenseAndPendingAmounts() {
        MonthlyPlanItem salary = item(TransactionType.INCOME, "Salario", "5000.00", "0.00", MonthlyPlanItemStatus.PENDING);
        MonthlyPlanItem rent = item(TransactionType.EXPENSE, "Aluguel", "1800.00", "0.00", MonthlyPlanItemStatus.PENDING);

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(salary, rent), List.of());

        assertAll(
                () -> assertMoney("5000.00", summary.plannedIncomeTotal()),
                () -> assertMoney("1800.00", summary.plannedExpenseTotal()),
                () -> assertMoney("3200.00", summary.plannedAvailableAmount()),
                () -> assertMoney("5000.00", summary.pendingIncomeTotal()),
                () -> assertMoney("1800.00", summary.pendingExpenseTotal()),
                () -> assertEquals(2, summary.pendingItems())
        );
    }

    @Test
    void shouldSeparatePaidPartialAndPendingItems() {
        MonthlyPlanItem paidIncome = item(TransactionType.INCOME, "Freela", "1000.00", "1000.00", MonthlyPlanItemStatus.PAID);
        MonthlyPlanItem partialExpense = item(TransactionType.EXPENSE, "Condominio", "800.00", "300.00", MonthlyPlanItemStatus.PARTIALLY_PAID);
        MonthlyPlanItem pendingExpense = item(TransactionType.EXPENSE, "Internet", "120.00", "0.00", MonthlyPlanItemStatus.PENDING);

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(paidIncome, partialExpense, pendingExpense), List.of());

        assertAll(
                () -> assertMoney("1000.00", summary.paidIncomeTotal()),
                () -> assertMoney("300.00", summary.paidExpenseTotal()),
                () -> assertMoney("620.00", summary.pendingExpenseTotal()),
                () -> assertEquals(2, summary.pendingItems()),
                () -> assertEquals(1, summary.paidItems())
        );
    }

    @Test
    void shouldIgnoreCanceledItems() {
        MonthlyPlanItem canceled = item(TransactionType.EXPENSE, "Cancelada", "900.00", "900.00", MonthlyPlanItemStatus.CANCELED);

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(canceled), List.of());

        assertAll(
                () -> assertMoney("0.00", summary.plannedExpenseTotal()),
                () -> assertMoney("0.00", summary.paidExpenseTotal()),
                () -> assertEquals(0, summary.pendingItems()),
                () -> assertEquals(0, summary.paidItems())
        );
    }

    @Test
    void shouldKeepInvoiceChildrenOutOfMainTotals() {
        MonthlyPlanItem invoice = item(TransactionType.EXPENSE, "Fatura", "1000.00", "0.00", MonthlyPlanItemStatus.PENDING);
        invoice.setAggregationType(MonthlyPlanItemAggregationType.GROUP_PARENT);
        invoice.setNature(MonthlyPlanItemNature.CREDIT_CARD);

        MonthlyPlanItem child = item(TransactionType.EXPENSE, "Mercado no cartao", "400.00", "0.00", MonthlyPlanItemStatus.PENDING);
        child.setAggregationType(MonthlyPlanItemAggregationType.GROUP_CHILD);
        child.setParentItem(invoice);

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(invoice, child), List.of());

        assertAll(
                () -> assertMoney("1000.00", summary.plannedExpenseTotal()),
                () -> assertMoney("1000.00", summary.pendingExpenseTotal()),
                () -> assertEquals(1, summary.pendingItems())
        );
    }

    @Test
    void shouldIncludeUnplannedTransactionsInProjectionWithoutDuplicatingLinkedTransactions() {
        MonthlyPlanItem rent = item(TransactionType.EXPENSE, "Aluguel", "1000.00", "1000.00", MonthlyPlanItemStatus.PAID);
        FinancialTransaction linkedRent = transaction(TransactionType.EXPENSE, "Aluguel", "1000.00");
        linkedRent.setMonthlyPlanItem(rent);
        FinancialTransaction unplannedExpense = transaction(TransactionType.EXPENSE, "Farmacia", "150.00");
        FinancialTransaction unplannedIncome = transaction(TransactionType.INCOME, "Bonus", "250.00");

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(rent), List.of(linkedRent, unplannedExpense, unplannedIncome));

        assertAll(
                () -> assertMoney("1150.00", summary.realizedExpenseTotal()),
                () -> assertMoney("250.00", summary.realizedIncomeTotal()),
                () -> assertMoney("250.00", summary.unplannedIncomeTotal()),
                () -> assertMoney("150.00", summary.unplannedExpenseTotal()),
                () -> assertMoney("-900.00", summary.projectedAvailableAmount())
        );
    }

    @Test
    void shouldIgnoreCreditCardPurchaseForProjectionWhenInvoiceExists() {
        MonthlyPlanItem invoice = item(TransactionType.EXPENSE, "Fatura Nubank", "700.00", "0.00", MonthlyPlanItemStatus.PENDING);
        invoice.setAggregationType(MonthlyPlanItemAggregationType.GROUP_PARENT);
        invoice.setNature(MonthlyPlanItemNature.CREDIT_CARD);
        FinancialTransaction cardPurchase = transaction(TransactionType.EXPENSE, "Compra no cartao", "200.00");
        cardPurchase.setAccount(account(AccountType.CREDIT_CARD));

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(invoice), List.of(cardPurchase));

        assertAll(
                () -> assertMoney("700.00", summary.plannedExpenseTotal()),
                () -> assertMoney("200.00", summary.realizedExpenseTotal()),
                () -> assertMoney("0.00", summary.unplannedExpenseTotal()),
                () -> assertMoney("-700.00", summary.projectedAvailableAmount())
        );
    }

    @Test
    void shouldIncludeCreditCardPurchaseForProjectionWhenNoInvoiceExists() {
        FinancialTransaction cardPurchase = transaction(TransactionType.EXPENSE, "Compra no cartao", "200.00");
        cardPurchase.setAccount(account(AccountType.CREDIT_CARD));

        MonthlyPeriodSummaryResponse summary = calculator.calculate(period, List.of(), List.of(cardPurchase));

        assertAll(
                () -> assertMoney("200.00", summary.realizedExpenseTotal()),
                () -> assertMoney("200.00", summary.unplannedExpenseTotal()),
                () -> assertMoney("-200.00", summary.projectedAvailableAmount())
        );
    }

    private FinancialPeriod period() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Usuario");
        owner.setEmail("user@example.com");

        FinancialPeriod financialPeriod = new FinancialPeriod();
        financialPeriod.setId(10L);
        financialPeriod.setOwner(owner);
        financialPeriod.setName("Junho");
        financialPeriod.setStartDate(LocalDate.of(2026, 6, 1));
        financialPeriod.setEndDate(LocalDate.of(2026, 6, 30));
        financialPeriod.setTurnoverDay(1);
        financialPeriod.setStatus(FinancialPeriodStatus.OPEN);
        financialPeriod.setArchivedIncomeTotal(money("0.00"));
        financialPeriod.setArchivedExpenseTotal(money("0.00"));
        financialPeriod.setArchivedTransferTotal(money("0.00"));
        financialPeriod.setArchivedNetTotal(money("0.00"));
        return financialPeriod;
    }

    private MonthlyPlanItem item(TransactionType type, String description, String expectedAmount, String actualAmount, MonthlyPlanItemStatus status) {
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setFinancialPeriod(period);
        item.setType(type);
        item.setDescription(description);
        item.setExpectedAmount(money(expectedAmount));
        item.setActualAmount(money(actualAmount));
        item.setDueDate(LocalDate.of(2026, 6, 10));
        item.setStatus(status);
        item.setNature(MonthlyPlanItemNature.VARIABLE);
        item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        item.setRecurring(false);
        return item;
    }

    private FinancialTransaction transaction(TransactionType type, String description, String amount) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setFinancialPeriod(period);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setAmount(money(amount));
        transaction.setOccurredOn(LocalDate.of(2026, 6, 12));
        transaction.setAccount(account(AccountType.CHECKING));
        return transaction;
    }

    private Account account(AccountType type) {
        Account account = new Account();
        account.setName(type.name());
        account.setType(type);
        return account;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
