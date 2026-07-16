package br.com.kuntzedevprojects.money_master_2.services.finance.debt;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtCancelRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.Debt;
import br.com.kuntzedevprojects.money_master_2.entities.DebtInstallment;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.DebtAmortizationMethod;
import br.com.kuntzedevprojects.money_master_2.enums.DebtInstallmentStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.DebtRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class DebtService {

    private static final MathContext RATE_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

    private final DebtRepository debtRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final FinancialPeriodService financialPeriodService;

    public DebtService(
            DebtRepository debtRepository,
            MonthlyPlanItemRepository planItemRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            FinancialPeriodService financialPeriodService
    ) {
        this.debtRepository = debtRepository;
        this.planItemRepository = planItemRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<DebtResponse> list(String ownerEmail) {
        return debtRepository.findByOwnerEmailWithInstallments(ownerEmail)
                .stream()
                .map(DebtResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DebtResponse get(String ownerEmail, Long id) {
        return DebtResponse.from(findOwnedDebt(ownerEmail, id));
    }

    @Transactional(readOnly = true)
    public DebtSummaryResponse summary(String ownerEmail) {
        List<DebtResponse> debts = list(ownerEmail);
        List<DebtResponse> active = debts.stream()
                .filter(debt -> debt.status() == DebtStatus.ACTIVE || debt.status() == DebtStatus.PAID_OFF)
                .toList();
        BigDecimal originalPrincipal = active.stream()
                .map(DebtResponse::principalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstandingPrincipal = active.stream()
                .map(DebtResponse::outstandingPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstandingScheduled = active.stream()
                .map(DebtResponse::outstandingScheduledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyCommitment = active.stream()
                .map(debt -> debt.installments().stream()
                        .filter(installment -> installment.status() == DebtInstallmentStatus.PENDING
                                || installment.status() == DebtInstallmentStatus.OVERDUE
                                || installment.status() == DebtInstallmentStatus.PARTIALLY_PAID)
                        .findFirst()
                        .map(installment -> installment.pendingAmount())
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int overdueInstallments = active.stream()
                .mapToInt(DebtResponse::overdueInstallments)
                .sum();
        int activeDebts = (int) active.stream()
                .filter(debt -> debt.status() == DebtStatus.ACTIVE)
                .count();
        return new DebtSummaryResponse(
                normalizeMoney(originalPrincipal),
                normalizeMoney(outstandingPrincipal),
                normalizeMoney(outstandingScheduled),
                normalizeMoney(monthlyCommitment),
                activeDebts,
                overdueInstallments
        );
    }

    @Transactional
    public DebtResponse create(String ownerEmail, DebtCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        BigDecimal principal = requirePositive(request.principalAmount(), "O saldo devedor inicial deve ser maior que zero.");
        int count = normalizeCount(request.installmentCount());
        LocalDate firstDueDate = requireDate(request.firstDueDate(), "A data da primeira parcela e obrigatoria.");
        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        BigDecimal monthlyFee = normalizeNonNegative(request.monthlyFeeAmount(), "O encargo mensal nao pode ser negativo.");
        DebtAmortizationMethod method = request.amortizationMethod() == null
                ? (request.installmentAmount() == null ? DebtAmortizationMethod.CONSTANT_PRINCIPAL : DebtAmortizationMethod.MANUAL_INSTALLMENT)
                : request.amortizationMethod();
        BigDecimal monthlyRate = monthlyRate(request.annualInterestRate());
        BigDecimal installmentAmount = resolveInstallmentAmount(method, principal, count, monthlyRate, request.installmentAmount(), monthlyFee);

        Account account = request.accountId() == null ? accountService.getOrCreateDefaultAccount(ownerEmail) : accountService.findOwnedAccount(ownerEmail, request.accountId());
        Category category = request.categoryId() == null ? null : categoryService.findAvailableCategory(ownerEmail, request.categoryId());
        if (category != null && category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("A categoria da divida precisa ser de despesa.");
        }

        Debt debt = new Debt();
        debt.setOwner(owner);
        debt.setAccount(account);
        debt.setCategory(category);
        debt.setName(normalizeRequired(request.name(), "O nome da divida e obrigatorio."));
        debt.setType(request.type() == null ? DebtType.OTHER : request.type());
        debt.setStatus(DebtStatus.ACTIVE);
        debt.setAmortizationMethod(method);
        debt.setPrincipalAmount(principal);
        debt.setInstallmentAmount(installmentAmount);
        debt.setAnnualInterestRate(normalizePercent(request.annualInterestRate(), "A taxa anual nao pode ser negativa."));
        debt.setAnnualCetRate(normalizePercent(request.annualCetRate(), "O CET anual nao pode ser negativo."));
        debt.setMonthlyFeeAmount(monthlyFee.signum() == 0 ? null : monthlyFee);
        debt.setInstallmentCount(count);
        debt.setStartDate(startDate);
        debt.setFirstDueDate(firstDueDate);
        debt.setLastDueDate(firstDueDate.plusMonths(count - 1L));
        debt.setNotes(normalizeNullable(request.notes()));
        Debt saved = debtRepository.save(debt);

        createSchedule(ownerEmail, owner, saved, principal, count, firstDueDate, monthlyRate, installmentAmount, monthlyFee, category, account);
        return DebtResponse.from(saved);
    }

    @Transactional
    public DebtResponse cancel(String ownerEmail, Long id, DebtCancelRequest request) {
        Debt debt = findOwnedDebt(ownerEmail, id);
        if (debt.getStatus() == DebtStatus.CANCELED) {
            return DebtResponse.from(debt);
        }
        debt.setStatus(DebtStatus.CANCELED);
        debt.setCanceledAt(Instant.now());
        debt.setNotes(appendNote(debt.getNotes(), request == null ? null : request.reason()));
        debt.getInstallments().forEach(installment -> {
            MonthlyPlanItem planItem = installment.getMonthlyPlanItem();
            boolean hasPayment = planItem != null && normalizeMoney(planItem.getActualAmount()).signum() > 0;
            if (!hasPayment) {
                installment.setStatus(DebtInstallmentStatus.CANCELED);
                if (planItem != null) {
                    planItem.setStatus(MonthlyPlanItemStatus.CANCELED);
                }
            }
        });
        return DebtResponse.from(debt);
    }

    private void createSchedule(
            String ownerEmail,
            User owner,
            Debt debt,
            BigDecimal principal,
            int count,
            LocalDate firstDueDate,
            BigDecimal monthlyRate,
            BigDecimal installmentAmount,
            BigDecimal monthlyFee,
            Category category,
            Account account
    ) {
        List<BigDecimal> constantPrincipalAmounts = allocateEvenly(principal, count);
        BigDecimal remaining = principal;
        for (int index = 0; index < count; index++) {
            int number = index + 1;
            LocalDate dueDate = firstDueDate.plusMonths(index);
            BigDecimal interest = monthlyRate.signum() == 0
                    ? BigDecimal.ZERO.setScale(2)
                    : remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart;
            if (debt.getAmortizationMethod() == DebtAmortizationMethod.CONSTANT_PRINCIPAL) {
                principalPart = constantPrincipalAmounts.get(index);
            } else {
                principalPart = installmentAmount.subtract(interest).subtract(monthlyFee).setScale(2, RoundingMode.HALF_UP);
                if (number == count) {
                    principalPart = remaining;
                }
                if (principalPart.compareTo(remaining) > 0) {
                    principalPart = remaining;
                }
            }
            if (principalPart.signum() <= 0) {
                throw new BusinessException("A parcela informada nao amortiza a divida. Aumente a parcela ou revise a taxa de juros.");
            }
            BigDecimal total = principalPart.add(interest).add(monthlyFee).setScale(2, RoundingMode.HALF_UP);
            remaining = remaining.subtract(principalPart).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            FinancialPeriod cycle = financialPeriodService.findOrCreateForDate(ownerEmail, dueDate);
            MonthlyPlanItem planItem = createMonthlyPlanItem(owner, debt, cycle, dueDate, number, total, principalPart, interest, monthlyFee, category, account);

            DebtInstallment installment = new DebtInstallment();
            installment.setOwner(owner);
            installment.setDebt(debt);
            installment.setFinancialPeriod(cycle);
            installment.setMonthlyPlanItem(planItem);
            installment.setInstallmentNumber(number);
            installment.setDueDate(dueDate);
            installment.setPrincipalAmount(principalPart);
            installment.setInterestAmount(interest);
            installment.setFeeAmount(monthlyFee);
            installment.setTotalAmount(total);
            installment.setBalanceAfterPayment(remaining);
            installment.setStatus(DebtInstallmentStatus.PENDING);
            installment.setNotes("Parcela " + number + " de " + count + " da divida #" + debt.getId() + ".");
            debt.addInstallment(installment);
        }
    }

    private MonthlyPlanItem createMonthlyPlanItem(
            User owner,
            Debt debt,
            FinancialPeriod cycle,
            LocalDate dueDate,
            int installmentNumber,
            BigDecimal total,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal fee,
            Category category,
            Account account
    ) {
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setOwner(owner);
        item.setFinancialPeriod(cycle);
        item.setAccount(account);
        item.setCategory(category);
        item.setType(TransactionType.EXPENSE);
        item.setDescription(debt.getName() + " (" + installmentNumber + "/" + debt.getInstallmentCount() + ")");
        item.setExpectedAmount(total);
        item.setActualAmount(BigDecimal.ZERO.setScale(2));
        item.setDueDate(clampDate(dueDate, cycle));
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(MonthlyPlanItemNature.DEBT);
        item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        item.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        item.setPaidByParent(false);
        item.setRecurring(false);
        item.setNotes("Parcela gerada automaticamente pela divida #" + debt.getId()
                + ". Principal: " + principal + "; juros: " + interest + "; encargos: " + fee + ".");
        return planItemRepository.save(item);
    }

    private BigDecimal resolveInstallmentAmount(
            DebtAmortizationMethod method,
            BigDecimal principal,
            int count,
            BigDecimal monthlyRate,
            BigDecimal requestedInstallment,
            BigDecimal monthlyFee
    ) {
        if (requestedInstallment != null) {
            return requirePositive(requestedInstallment, "O valor da parcela deve ser maior que zero.");
        }
        if (method == DebtAmortizationMethod.PRICE && monthlyRate.signum() > 0) {
            BigDecimal onePlusRatePower = BigDecimal.ONE.add(monthlyRate).pow(count, RATE_CONTEXT);
            BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRatePower, RATE_CONTEXT);
            BigDecimal denominator = onePlusRatePower.subtract(BigDecimal.ONE, RATE_CONTEXT);
            return numerator.divide(denominator, 2, RoundingMode.HALF_UP).add(monthlyFee).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal basePrincipal = principal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal firstInterest = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        return basePrincipal.add(firstInterest).add(monthlyFee).setScale(2, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> allocateEvenly(BigDecimal total, int count) {
        BigDecimal base = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal accumulated = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            BigDecimal amount = index == count - 1
                    ? total.subtract(accumulated).setScale(2, RoundingMode.HALF_UP)
                    : base;
            values.add(amount);
            accumulated = accumulated.add(amount).setScale(2, RoundingMode.HALF_UP);
        }
        return values;
    }

    private Debt findOwnedDebt(String ownerEmail, Long id) {
        return debtRepository.findByIdAndOwnerEmailWithInstallments(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Divida nao encontrada."));
    }

    private BigDecimal monthlyRate(BigDecimal annualRate) {
        BigDecimal normalized = normalizePercent(annualRate, "A taxa anual nao pode ser negativa.");
        if (normalized.signum() == 0) {
            return BigDecimal.ZERO.setScale(8);
        }
        return normalized.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
    }

    private int normalizeCount(Integer count) {
        if (count == null || count < 1) {
            throw new BusinessException("A quantidade de parcelas deve ser maior que zero.");
        }
        if (count > 420) {
            throw new BusinessException("A quantidade de parcelas nao pode ultrapassar 420.");
        }
        return count;
    }

    private LocalDate requireDate(LocalDate value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        return value;
    }

    private BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNonNegative(BigDecimal value, String message) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (value.signum() < 0) {
            throw new BusinessException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePercent(BigDecimal value, String message) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4);
        }
        if (value.signum() < 0) {
            throw new BusinessException(message);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate clampDate(LocalDate dueDate, FinancialPeriod period) {
        if (dueDate.isBefore(period.getStartDate())) {
            return period.getStartDate();
        }
        if (dueDate.isAfter(period.getEndDate())) {
            return period.getEndDate();
        }
        return dueDate;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String appendNote(String note, String extra) {
        String normalizedExtra = normalizeNullable(extra);
        if (normalizedExtra == null) {
            return normalizeNullable(note);
        }
        String normalizedNote = normalizeNullable(note);
        return normalizedNote == null ? normalizedExtra : normalizedNote + "\nCancelamento: " + normalizedExtra;
    }
}
