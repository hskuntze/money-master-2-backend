package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.ComparativeReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.DailyCashFlowResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.report.MonthlySemanticReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.dashboard.MonthlyDashboardService;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentProductService;

@Service
public class FinancialReportService {

    private final FinancialTransactionRepository transactionRepository;
    private final AccountService accountService;
    private final MonthlyDashboardService monthlyDashboardService;
    private final SavingsJarService savingsJarService;
    private final InvestmentProductService investmentProductService;

    public FinancialReportService(
            FinancialTransactionRepository transactionRepository,
            AccountService accountService,
            MonthlyDashboardService monthlyDashboardService,
            SavingsJarService savingsJarService,
            InvestmentProductService investmentProductService
    ) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.monthlyDashboardService = monthlyDashboardService;
        this.savingsJarService = savingsJarService;
        this.investmentProductService = investmentProductService;
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse summary(String ownerEmail, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        BigDecimal income = transactionRepository.sumAmount(ownerEmail, null, from, to, null, TransactionType.INCOME);
        BigDecimal expense = transactionRepository.sumAmount(ownerEmail, null, from, to, null, TransactionType.EXPENSE);
        BigDecimal transfer = transactionRepository.sumAmount(ownerEmail, null, from, to, null, TransactionType.TRANSFER);
        long count = transactionRepository.search(ownerEmail, from, to, null, null, null, null, null).size();
        return new FinancialSummaryResponse(
                from,
                to,
                nullToZero(income),
                nullToZero(expense),
                nullToZero(transfer),
                nullToZero(income).subtract(nullToZero(expense)),
                count
        );
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceResponse> accountBalances(String ownerEmail) {
        return accountService.balances(ownerEmail);
    }

    @Transactional(readOnly = true)
    public List<DailyCashFlowResponse> dailyCashFlow(String ownerEmail, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        List<FinancialTransaction> transactions = transactionRepository.search(ownerEmail, from, to, null, null, null, null, null);
        Map<LocalDate, List<FinancialTransaction>> byDate = transactions.stream()
                .collect(Collectors.groupingBy(FinancialTransaction::getOccurredOn, TreeMap::new, Collectors.toList()));

        return byDate.entrySet()
                .stream()
                .map(entry -> toDailyCashFlow(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryReportResponse> byCategory(String ownerEmail, LocalDate from, LocalDate to, TransactionType type) {
        validatePeriod(from, to);
        List<FinancialTransaction> transactions = transactionRepository.search(ownerEmail, from, to, null, null, type, null, null);

        return transactions.stream()
                .filter(transaction -> transaction.getCategory() != null)
                .collect(Collectors.groupingBy(FinancialTransaction::getCategory))
                .entrySet()
                .stream()
                .map(entry -> toCategoryReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategoryReportResponse::total).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ComparativeReportResponse compare(
            String ownerEmail,
            LocalDate fromA,
            LocalDate toA,
            LocalDate fromB,
            LocalDate toB
    ) {
        FinancialSummaryResponse first = summary(ownerEmail, fromA, toA);
        FinancialSummaryResponse second = summary(ownerEmail, fromB, toB);

        return new ComparativeReportResponse(
                first,
                second,
                second.incomeTotal().subtract(first.incomeTotal()),
                second.expenseTotal().subtract(first.expenseTotal()),
                second.netResult().subtract(first.netResult())
        );
    }

    @Transactional(readOnly = true)
    public MonthlySemanticReportResponse monthlySemantic(String ownerEmail, Long cycleId) {
        return monthlySemantic(ownerEmail, cycleId, true);
    }

    @Transactional(readOnly = true)
    public MonthlySemanticReportResponse monthlySemantic(String ownerEmail, Long cycleId, boolean includeInvestmentDetails) {
        MonthlyDashboardResponse dashboard = monthlyDashboardService.get(ownerEmail, cycleId);
        SavingsJarSummaryResponse savings = savingsJarService.summary(ownerEmail);
        List<InvestmentProductResponse> investments = includeInvestmentDetails
                ? investmentProductService.list(ownerEmail)
                : List.of();
        LocalDate from = dashboard.cycle().startDate();
        LocalDate to = dashboard.cycle().endDate();

        return new MonthlySemanticReportResponse(
                dashboard.cycle(),
                new MonthlySemanticReportResponse.PlanningSection(
                        dashboard.plannedIncomeTotal(),
                        dashboard.plannedPayablesTotal(),
                        dashboard.creditCardInvoicesTotal(),
                        dashboard.savingsPlannedTotal(),
                        dashboard.investmentPlannedTotal(),
                        dashboard.plannedAvailableAmount()
                ),
                new MonthlySemanticReportResponse.RealizedSection(
                        dashboard.receivedIncomeTotal(),
                        dashboard.paidPayablesTotal(),
                        dashboard.creditCardInvoicesPaidTotal(),
                        dashboard.savingsActualTotal(),
                        dashboard.investmentActualTotal(),
                        dashboard.unplannedIncomeTotal(),
                        dashboard.unplannedExpenseTotal(),
                        dashboard.realizedAvailableAmount()
                ),
                new MonthlySemanticReportResponse.CreditCardSection(
                        dashboard.creditCardInvoicesTotal(),
                        dashboard.creditCardInvoicesPaidTotal(),
                        dashboard.creditCardInvoicesPendingTotal()
                ),
                new MonthlySemanticReportResponse.InstallmentSection(
                        dashboard.installmentsCurrentMonthTotal(),
                        dashboard.installmentsFutureTotal(),
                        dashboard.anticipatedInstallmentsTotal()
                ),
                new MonthlySemanticReportResponse.SavingsJarSection(
                        savings.totalSaved(),
                        savings.totalTarget(),
                        dashboard.savingsPlannedTotal(),
                        dashboard.savingsActualTotal(),
                        savings.totalYield(),
                        savings.averageProgressPercentage()
                ),
                investmentSection(dashboard, investments, includeInvestmentDetails),
                dashboard.cashBalanceCurrent(),
                dashboard.projectedAvailableAmount(),
                dailyCashFlow(ownerEmail, from, to),
                byCategory(ownerEmail, from, to, TransactionType.EXPENSE),
                byCategory(ownerEmail, from, to, TransactionType.INCOME),
                dashboard.alerts()
        );
    }

    private MonthlySemanticReportResponse.InvestmentSection investmentSection(
            MonthlyDashboardResponse dashboard,
            List<InvestmentProductResponse> investments,
            boolean includeInvestmentDetails
    ) {
        if (!includeInvestmentDetails) {
            return new MonthlySemanticReportResponse.InvestmentSection(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0
            );
        }
        return new MonthlySemanticReportResponse.InvestmentSection(
                investments.stream().map(InvestmentProductResponse::currentAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                investments.stream().map(InvestmentProductResponse::totalContributed).reduce(BigDecimal.ZERO, BigDecimal::add),
                investments.stream().map(InvestmentProductResponse::totalWithdrawn).reduce(BigDecimal.ZERO, BigDecimal::add),
                investments.stream().map(InvestmentProductResponse::totalYield).reduce(BigDecimal.ZERO, BigDecimal::add),
                dashboard.investmentPlannedTotal(),
                dashboard.investmentActualTotal(),
                investments.stream().filter(InvestmentProductResponse::active).count()
        );
    }

    private DailyCashFlowResponse toDailyCashFlow(LocalDate date, List<FinancialTransaction> transactions) {
        BigDecimal income = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.INCOME)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DailyCashFlowResponse(date, income, expense, income.subtract(expense));
    }

    private CategoryReportResponse toCategoryReport(Category category, List<FinancialTransaction> transactions) {
        BigDecimal total = transactions.stream()
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CategoryReportResponse(category.getId(), category.getName(), category.getType(), total, transactions.size());
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
