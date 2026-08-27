package br.com.kuntzedevprojects.money_master_2.services.finance.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardAlertResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardBalanceLineResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardBalanceOverviewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardBalanceViewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardResponse;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseEntryRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class MonthlyDashboardService {

    private final FinancialPeriodService financialPeriodService;
    private final CreditCardInvoiceRepository creditCardInvoiceRepository;
    private final InstallmentPurchaseEntryRepository installmentEntryRepository;
    private final AccountService accountService;

    public MonthlyDashboardService(
            FinancialPeriodService financialPeriodService,
            CreditCardInvoiceRepository creditCardInvoiceRepository,
            InstallmentPurchaseEntryRepository installmentEntryRepository,
            AccountService accountService
    ) {
        this.financialPeriodService = financialPeriodService;
        this.creditCardInvoiceRepository = creditCardInvoiceRepository;
        this.installmentEntryRepository = installmentEntryRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public MonthlyDashboardResponse get(String ownerEmail, Long cycleId) {
        MonthlyPeriodSummaryResponse legacySummary = financialPeriodService.summary(ownerEmail, cycleId);
        List<MonthlyPlanItemResponse> items = financialPeriodService.listPlanItems(ownerEmail, cycleId, null);
        List<CreditCardInvoice> invoices = creditCardInvoiceRepository.search(ownerEmail, null, cycleId);
        List<InstallmentPurchaseEntry> entries = installmentEntryRepository.findActiveEntriesByOwnerEmail(ownerEmail);
        List<AccountBalanceResponse> balances = accountService.balances(ownerEmail);

        BigDecimal plannedPayables = sumItems(items, MonthlyDashboardService::isCommonPayable, MonthlyPlanItemResponse::expectedAmount);
        BigDecimal paidPayables = sumItems(items, MonthlyDashboardService::isCommonPayable, MonthlyPlanItemResponse::actualAmount);
        BigDecimal pendingPayables = plannedPayables.subtract(paidPayables).max(zero()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal savingsPlanned = sumItems(items, MonthlyDashboardService::isSavingsPayable, MonthlyPlanItemResponse::expectedAmount);
        BigDecimal savingsActual = sumItems(items, MonthlyDashboardService::isSavingsPayable, MonthlyPlanItemResponse::actualAmount);
        BigDecimal investmentPlanned = sumItems(items, MonthlyDashboardService::isInvestmentPayable, MonthlyPlanItemResponse::expectedAmount);
        BigDecimal investmentActual = sumItems(items, MonthlyDashboardService::isInvestmentPayable, MonthlyPlanItemResponse::actualAmount);

        BigDecimal invoiceTotal = sumInvoices(invoices, false);
        BigDecimal invoicePaid = sumInvoices(invoices, true);
        if (invoiceTotal.signum() == 0) {
            invoiceTotal = sumItems(items, MonthlyDashboardService::isCreditCardPayable, MonthlyPlanItemResponse::expectedAmount);
            invoicePaid = sumItems(items, MonthlyDashboardService::isCreditCardPayable, MonthlyPlanItemResponse::actualAmount);
        }
        BigDecimal invoicePending = invoiceTotal.subtract(invoicePaid).max(zero()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal plannedExpenseTotal = plannedPayables
                .add(invoiceTotal)
                .add(savingsPlanned)
                .add(investmentPlanned)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingSavings = savingsPlanned.subtract(savingsActual).max(zero()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingInvestments = investmentPlanned.subtract(investmentActual).max(zero()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingExpenseTotal = pendingPayables
                .add(invoicePending)
                .add(pendingSavings)
                .add(pendingInvestments)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal installmentsCurrent = sumEntries(entries.stream()
                .filter(entry -> entry.getFinancialPeriod() != null && cycleId.equals(entry.getFinancialPeriod().getId()))
                .filter(MonthlyDashboardService::isOpenInstallment)
                .toList());
        BigDecimal installmentsFuture = sumEntries(entries.stream()
                .filter(entry -> entry.getFinancialPeriod() != null && entry.getFinancialPeriod().getStartDate().isAfter(legacySummary.period().endDate()))
                .filter(MonthlyDashboardService::isOpenInstallment)
                .toList());
        BigDecimal anticipatedInstallments = sumEntries(entries.stream()
                .filter(InstallmentPurchaseEntry::isAnticipated)
                .toList());

        BigDecimal cashBalance = balances.stream()
                .map(AccountBalanceResponse::currentBalance)
                .map(MonthlyDashboardService::nullToZero)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal realizedAvailable = legacySummary.realizedIncomeTotal()
                .subtract(legacySummary.realizedExpenseTotal())
                .setScale(2, RoundingMode.HALF_UP);
        MonthlyDashboardBalanceOverviewResponse balanceOverview = buildBalanceOverview(
                legacySummary,
                plannedPayables,
                paidPayables,
                pendingPayables,
                invoiceTotal,
                invoicePaid,
                invoicePending,
                savingsPlanned,
                savingsActual,
                investmentPlanned,
                investmentActual,
                plannedExpenseTotal,
                pendingExpenseTotal,
                cashBalance,
                realizedAvailable
        );

        List<MonthlyDashboardAlertResponse> alerts = buildAlerts(
                items,
                invoices,
                entries,
                legacySummary.projectedAvailableAmount(),
                savingsPlanned,
                savingsActual,
                investmentPlanned,
                investmentActual
        );

        return new MonthlyDashboardResponse(
                MonthlyCycleResponse.from(legacySummary.period()),
                legacySummary.plannedIncomeTotal(),
                legacySummary.paidIncomeTotal(),
                legacySummary.pendingIncomeTotal(),
                plannedPayables,
                paidPayables,
                pendingPayables,
                invoiceTotal,
                invoicePaid,
                invoicePending,
                installmentsCurrent,
                installmentsFuture,
                anticipatedInstallments,
                savingsPlanned,
                savingsActual,
                investmentPlanned,
                investmentActual,
                legacySummary.unplannedIncomeTotal(),
                legacySummary.unplannedExpenseTotal(),
                cashBalance,
                legacySummary.plannedAvailableAmount(),
                realizedAvailable,
                legacySummary.projectedAvailableAmount(),
                balanceOverview,
                alerts
        );
    }

    private static MonthlyDashboardBalanceOverviewResponse buildBalanceOverview(
            MonthlyPeriodSummaryResponse legacySummary,
            BigDecimal plannedPayables,
            BigDecimal paidPayables,
            BigDecimal pendingPayables,
            BigDecimal invoiceTotal,
            BigDecimal invoicePaid,
            BigDecimal invoicePending,
            BigDecimal savingsPlanned,
            BigDecimal savingsActual,
            BigDecimal investmentPlanned,
            BigDecimal investmentActual,
            BigDecimal plannedExpenseTotal,
            BigDecimal pendingExpenseTotal,
            BigDecimal cashBalance,
            BigDecimal realizedAvailable
    ) {
        MonthlyDashboardBalanceViewResponse planning = new MonthlyDashboardBalanceViewResponse(
                "PLANNING",
                "Planejamento",
                "Combina receitas previstas, compromissos do mes, faturas, cofrinhos e baixas ja realizadas.",
                nullToZero(legacySummary.plannedIncomeTotal()),
                plannedExpenseTotal,
                nullToZero(legacySummary.pendingIncomeTotal()),
                pendingExpenseTotal,
                nullToZero(legacySummary.projectedAvailableAmount()),
                List.of(
                        line("PLANNED_INCOME", "Receitas planejadas", legacySummary.plannedIncomeTotal(), "ADDS",
                                "Total de receitas previstas para o ciclo."),
                        line("COMMON_PAYABLES", "Contas planejadas", plannedPayables, "SUBTRACTS",
                                "Despesas comuns do planejamento, sem faturas de cartao e sem cofrinhos."),
                        line("CREDIT_CARD_INVOICES", "Faturas do mes", invoiceTotal, "SUBTRACTS",
                                "Valor esperado ou confirmado das faturas vinculadas ao ciclo."),
                        line("SAVINGS_GOALS", "Aportes em cofrinhos", savingsPlanned, "SUBTRACTS",
                                "Intencoes de guardar dinheiro tratadas como compromisso do ciclo."),
                        line("INVESTMENT_CONTRIBUTIONS", "Aportes em investimentos", investmentPlanned, "SUBTRACTS",
                                "Aportes planejados para produtos financeiros acompanhados."),
                        line("PENDING_INCOME", "Receitas pendentes", legacySummary.pendingIncomeTotal(), "INFO",
                                "Parte das receitas planejadas que ainda nao recebeu baixa."),
                        line("PENDING_EXPENSES", "Compromissos pendentes", pendingExpenseTotal, "INFO",
                                "Contas, faturas e aportes planejados que ainda nao foram realizados por completo.")
                )
        );

        MonthlyDashboardBalanceViewResponse cash = new MonthlyDashboardBalanceViewResponse(
                "CASH",
                "Caixa",
                "Mostra o dinheiro atualmente registrado nas contas e o resultado efetivamente realizado no ciclo.",
                nullToZero(legacySummary.realizedIncomeTotal()),
                nullToZero(legacySummary.realizedExpenseTotal()),
                zero(),
                zero(),
                cashBalance,
                List.of(
                        line("CURRENT_ACCOUNT_BALANCE", "Saldo atual em contas", cashBalance, "INFO",
                                "Soma dos saldos atuais das contas ativas acompanhadas."),
                        line("RECEIVED_INCOME", "Receitas recebidas", legacySummary.realizedIncomeTotal(), "ADDS",
                                "Receitas planejadas ou avulsas que ja foram registradas como recebidas no ciclo."),
                        line("PAID_PAYABLES", "Contas pagas", paidPayables, "SUBTRACTS",
                                "Despesas comuns do planejamento que ja receberam baixa."),
                        line("PAID_CREDIT_CARD_INVOICES", "Faturas pagas", invoicePaid, "SUBTRACTS",
                                "Pagamentos registrados nas faturas do ciclo."),
                        line("SAVINGS_DONE", "Aportes realizados", savingsActual, "SUBTRACTS",
                                "Valor efetivamente separado nos cofrinhos durante o ciclo."),
                        line("INVESTMENTS_DONE", "Investimentos realizados", investmentActual, "SUBTRACTS",
                                "Valor efetivamente aportado em produtos financeiros durante o ciclo."),
                        line("REALIZED_RESULT", "Resultado realizado", realizedAvailable, "INFO",
                                "Receitas realizadas menos despesas realizadas do ciclo.")
                )
        );

        return new MonthlyDashboardBalanceOverviewResponse(
                planning,
                cash,
                nullToZero(legacySummary.projectedAvailableAmount()).subtract(cashBalance).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private static MonthlyDashboardBalanceLineResponse line(String key, String label, BigDecimal amount, String effect, String explanation) {
        return new MonthlyDashboardBalanceLineResponse(key, label, nullToZero(amount), effect, explanation);
    }

    private static List<MonthlyDashboardAlertResponse> buildAlerts(
            List<MonthlyPlanItemResponse> items,
            List<CreditCardInvoice> invoices,
            List<InstallmentPurchaseEntry> entries,
            BigDecimal projectedAvailable,
            BigDecimal savingsPlanned,
            BigDecimal savingsActual,
            BigDecimal investmentPlanned,
            BigDecimal investmentActual
    ) {
        LocalDate today = LocalDate.now();
        List<MonthlyDashboardAlertResponse> alerts = new ArrayList<>();

        if (projectedAvailable.signum() < 0) {
            alerts.add(alert("PROJECTED_BALANCE_NEGATIVE", "HIGH", "Saldo projetado negativo",
                    "O saldo projetado do ciclo esta abaixo de zero.", null, null, null, projectedAvailable));
        }

        items.stream()
                .filter(MonthlyDashboardService::isCommonPayable)
                .filter(item -> item.status() == MonthlyPlanItemStatus.PENDING || item.status() == MonthlyPlanItemStatus.PARTIALLY_PAID)
                .filter(item -> item.dueDate() != null && item.dueDate().isBefore(today))
                .limit(5)
                .forEach(item -> alerts.add(alert("PAYABLE_OVERDUE", "HIGH", "Conta vencida",
                        item.description() + " venceu em " + item.dueDate() + ".", "MONTHLY_PAYABLE", item.id(), item.dueDate(), item.expectedAmount())));

        invoices.stream()
                .filter(invoice -> invoice.getStatus() != CreditCardInvoiceStatus.CANCELED && invoice.getStatus() != CreditCardInvoiceStatus.PAID)
                .filter(invoice -> invoice.getDueDate() != null && !invoice.getDueDate().isBefore(today) && !invoice.getDueDate().isAfter(today.plusDays(5)))
                .limit(5)
                .forEach(invoice -> alerts.add(alert("INVOICE_DUE_SOON", "MEDIUM", "Fatura perto do vencimento",
                        invoice.getCreditCard().getName() + " vence em " + invoice.getDueDate() + ".", "CREDIT_CARD_INVOICE", invoice.getId(), invoice.getDueDate(), invoiceAmount(invoice))));

        if (savingsPlanned.signum() > 0 && savingsActual.compareTo(savingsPlanned) < 0) {
            alerts.add(alert("SAVINGS_BELOW_MONTHLY_GOAL", "MEDIUM", "Cofrinho abaixo da meta mensal",
                    "Ainda falta aportar " + savingsPlanned.subtract(savingsActual).setScale(2, RoundingMode.HALF_UP) + " no ciclo.", "SAVINGS_JAR", null, null, savingsPlanned.subtract(savingsActual)));
        }

        if (investmentPlanned.signum() > 0 && investmentActual.compareTo(investmentPlanned) < 0) {
            alerts.add(alert("INVESTMENT_CONTRIBUTION_PENDING", "LOW", "Aporte de investimento pendente",
                    "Ainda falta aportar " + investmentPlanned.subtract(investmentActual).setScale(2, RoundingMode.HALF_UP) + " em investimentos no ciclo.", "INVESTMENT", null, null, investmentPlanned.subtract(investmentActual)));
        }

        entries.stream()
                .filter(InstallmentPurchaseEntry::isAnticipated)
                .limit(3)
                .forEach(entry -> alerts.add(alert("INSTALLMENT_ANTICIPATED", "LOW", "Parcela antecipada",
                        entry.getPurchase().getDescription() + " teve parcela antecipada.", "INSTALLMENT", entry.getId(), entry.getDueDate(), entry.getAmount())));

        return alerts;
    }

    private static MonthlyDashboardAlertResponse alert(String type, String severity, String title, String message, String entityType, Long entityId, LocalDate dueDate, BigDecimal amount) {
        return new MonthlyDashboardAlertResponse(type, severity, title, message, entityType, entityId, dueDate, nullToZero(amount));
    }

    private static boolean isCommonPayable(MonthlyPlanItemResponse item) {
        return item.type() == TransactionType.EXPENSE
                && item.status() != MonthlyPlanItemStatus.CANCELED
                && item.aggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD
                && item.nature() != MonthlyPlanItemNature.CREDIT_CARD
                && item.nature() != MonthlyPlanItemNature.SAVINGS_JAR
                && item.nature() != MonthlyPlanItemNature.INVESTMENT;
    }

    private static boolean isCreditCardPayable(MonthlyPlanItemResponse item) {
        return item.type() == TransactionType.EXPENSE
                && item.status() != MonthlyPlanItemStatus.CANCELED
                && item.aggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD
                && item.nature() == MonthlyPlanItemNature.CREDIT_CARD;
    }

    private static boolean isSavingsPayable(MonthlyPlanItemResponse item) {
        return item.type() == TransactionType.EXPENSE
                && item.status() != MonthlyPlanItemStatus.CANCELED
                && item.aggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD
                && item.nature() == MonthlyPlanItemNature.SAVINGS_JAR;
    }

    private static boolean isInvestmentPayable(MonthlyPlanItemResponse item) {
        return item.type() == TransactionType.EXPENSE
                && item.status() != MonthlyPlanItemStatus.CANCELED
                && item.aggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD
                && item.nature() == MonthlyPlanItemNature.INVESTMENT;
    }

    private static boolean isOpenInstallment(InstallmentPurchaseEntry entry) {
        return entry.getStatus() == InstallmentEntryStatus.PENDING
                || entry.getStatus() == InstallmentEntryStatus.POSTED
                || entry.getStatus() == InstallmentEntryStatus.IN_INVOICE;
    }

    private static BigDecimal sumInvoices(List<CreditCardInvoice> invoices, boolean paid) {
        return invoices.stream()
                .filter(invoice -> invoice.getStatus() != CreditCardInvoiceStatus.CANCELED)
                .map(paid ? CreditCardInvoice::getPaidAmount : MonthlyDashboardService::invoiceAmount)
                .map(MonthlyDashboardService::nullToZero)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal invoiceAmount(CreditCardInvoice invoice) {
        BigDecimal finalAmount = nullToZero(invoice.getFinalAmount());
        return finalAmount.signum() > 0 ? finalAmount : nullToZero(invoice.getExpectedAmount());
    }

    private static BigDecimal sumEntries(List<InstallmentPurchaseEntry> entries) {
        return entries.stream()
                .map(InstallmentPurchaseEntry::getAmount)
                .map(MonthlyDashboardService::nullToZero)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sumItems(List<MonthlyPlanItemResponse> items, java.util.function.Predicate<MonthlyPlanItemResponse> filter, java.util.function.Function<MonthlyPlanItemResponse, BigDecimal> value) {
        return items.stream()
                .filter(filter)
                .map(value)
                .map(MonthlyDashboardService::nullToZero)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
