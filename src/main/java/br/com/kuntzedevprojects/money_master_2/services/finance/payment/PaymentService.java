package br.com.kuntzedevprojects.money_master_2.services.finance.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentReverseRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarMovementRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.Payment;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentSource;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;
import br.com.kuntzedevprojects.money_master_2.services.finance.savings.SavingsJarContributionPlanService;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final FinancialPeriodService financialPeriodService;
    private final FinancialTransactionService transactionService;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final SavingsJarService savingsJarService;

    public PaymentService(
            PaymentRepository paymentRepository,
            FinancialTransactionRepository transactionRepository,
            FinancialPeriodService financialPeriodService,
            FinancialTransactionService transactionService,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            SavingsJarService savingsJarService
    ) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.financialPeriodService = financialPeriodService;
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.savingsJarService = savingsJarService;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> search(String ownerEmail, Long cycleId, LocalDate from, LocalDate to) {
        return paymentRepository.search(ownerEmail, cycleId, from, to)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayablePayments(String ownerEmail, Long payableId) {
        ensurePayable(ownerEmail, payableId);
        return paymentRepository.findByPayableId(ownerEmail, payableId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listIncomeReceipts(String ownerEmail, Long incomePlanId) {
        ensureIncomePlan(ownerEmail, incomePlanId);
        return paymentRepository.findByIncomePlanId(ownerEmail, incomePlanId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse registerPayablePayment(String ownerEmail, Long payableId, PaymentRequest request) {
        MonthlyPlanItem payable = ensurePayable(ownerEmail, payableId);
        Payment payment = register(ownerEmail, payable, null, request);
        applyLegacyPaymentState(payable);
        applySavingsJarContribution(ownerEmail, payable, payment, false);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse registerIncomeReceipt(String ownerEmail, Long incomePlanId, PaymentRequest request) {
        MonthlyPlanItem incomePlan = ensureIncomePlan(ownerEmail, incomePlanId);
        Payment payment = register(ownerEmail, null, incomePlan, request);
        applyLegacyPaymentState(incomePlan);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse reverse(String ownerEmail, Long paymentId, PaymentReverseRequest request) {
        Payment payment = findOwnedPayment(ownerEmail, paymentId);
        MonthlyPlanItem target = payment.getPayable() == null ? payment.getIncomePlan() : payment.getPayable();
        if (payment.getStatus() != PaymentStatus.ACTIVE) {
            throw new BusinessException("Este pagamento ja foi revertido ou cancelado.");
        }
        payment.setStatus(PaymentStatus.REVERSED);
        payment.setReversedAt(Instant.now());
        payment.setNotes(appendNote(payment.getNotes(), request == null ? null : request.notes()));

        FinancialTransaction transaction = payment.getTransaction();
        if (transaction != null) {
            if (Boolean.TRUE.equals(request == null ? null : request.deleteLinkedTransaction())) {
                transactionRepository.delete(transaction);
                transactionRepository.flush();
            } else {
                transaction.setMonthlyPlanItem(null);
            }
        }
        applyLegacyPaymentState(target);
        applySavingsJarContribution(ownerEmail, target, payment, true);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void cancel(String ownerEmail, Long paymentId) {
        Payment payment = findOwnedPayment(ownerEmail, paymentId);
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return;
        }
        MonthlyPlanItem target = payment.getPayable() == null ? payment.getIncomePlan() : payment.getPayable();
        payment.setStatus(PaymentStatus.CANCELED);
        payment.setReversedAt(Instant.now());
        if (payment.getTransaction() != null) {
            payment.getTransaction().setMonthlyPlanItem(null);
        }
        applyLegacyPaymentState(target);
        applySavingsJarContribution(ownerEmail, target, payment, true);
    }

    @Transactional(readOnly = true)
    public Payment findOwnedPayment(String ownerEmail, Long paymentId) {
        return paymentRepository.findByIdAndOwnerEmail(paymentId, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado."));
    }

    private Payment register(String ownerEmail, MonthlyPlanItem payable, MonthlyPlanItem incomePlan, PaymentRequest request) {
        MonthlyPlanItem target = payable == null ? incomePlan : payable;
        ensureCanReceivePayment(target);
        PaymentRequest safeRequest = request == null
                ? new PaymentRequest(null, null, null, null, null, null, null, true, null)
                : request;

        BigDecimal amount = normalizeAmount(safeRequest.amount() == null ? remainingOrExpected(target) : safeRequest.amount());
        LocalDate paymentDate = safeRequest.paymentDate() == null ? LocalDate.now() : safeRequest.paymentDate();
        Account account = resolveAccount(ownerEmail, target, safeRequest.accountId());
        Category category = safeRequest.categoryId() == null ? target.getCategory() : categoryService.findAvailableCategory(ownerEmail, safeRequest.categoryId());
        FinancialTransaction transaction = resolveTransaction(ownerEmail, target, safeRequest, amount, paymentDate, account, category);

        Payment payment = new Payment();
        payment.setOwner(currentUserService.findUserByEmail(ownerEmail));
        payment.setCycle(target.getFinancialPeriod());
        payment.setPayable(payable);
        payment.setIncomePlan(incomePlan);
        payment.setTransaction(transaction);
        payment.setAccount(account);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setMethod(safeRequest.method() == null ? PaymentMethod.OTHER : safeRequest.method());
        payment.setSource(safeRequest.source() == null ? PaymentSource.MANUAL : safeRequest.source());
        payment.setStatus(PaymentStatus.ACTIVE);
        payment.setNotes(normalizeNullable(safeRequest.notes()));
        return paymentRepository.save(payment);
    }

    private FinancialTransaction resolveTransaction(
            String ownerEmail,
            MonthlyPlanItem item,
            PaymentRequest request,
            BigDecimal amount,
            LocalDate paymentDate,
            Account account,
            Category category
    ) {
        if (request.transactionId() != null) {
            FinancialTransaction transaction = transactionService.findOwnedTransaction(ownerEmail, request.transactionId());
            if (transaction.getType() != item.getType()) {
                throw new BusinessException("O tipo da transacao precisa ser igual ao tipo do item mensal.");
            }
            transaction.setMonthlyPlanItem(item);
            transaction.setFinancialPeriod(item.getFinancialPeriod());
            if (transaction.getCategory() == null && category != null) {
                transaction.setCategory(category);
            }
            return transaction;
        }
        if (Boolean.FALSE.equals(request.createTransaction())) {
            return null;
        }
        FinancialTransactionCreateRequest transactionRequest = new FinancialTransactionCreateRequest(
                account == null ? null : account.getId(),
                category == null ? null : category.getId(),
                item.getFinancialPeriod().getId(),
                item.getId(),
                item.getType(),
                item.getDescription(),
                amount,
                paymentDate,
                TransactionSource.MANUAL,
                appendNote(request.notes(), "Lancamento criado a partir de Payment.")
        );
        Long transactionId = transactionService.create(ownerEmail, transactionRequest).id();
        return transactionService.findOwnedTransaction(ownerEmail, transactionId);
    }

    private void applyLegacyPaymentState(MonthlyPlanItem item) {
        if (item == null) {
            return;
        }
        BigDecimal activePayments = item.getType() == TransactionType.INCOME
                ? paymentRepository.sumActiveByIncomePlan(item.getId())
                : paymentRepository.sumActiveByPayable(item.getId());
        BigDecimal normalized = nullToZero(activePayments);
        if (normalized.signum() == 0) {
            financialPeriodService.synchronizePlanItemPayment(item);
            return;
        }
        item.setActualAmount(normalized);
        item.setPaidOn(paymentRepository.findLatestActivePaymentDateByPlanItem(item.getId()));
        BigDecimal expected = nullToZero(item.getExpectedAmount());
        if (normalized.signum() <= 0) {
            item.setStatus(MonthlyPlanItemStatus.PENDING);
        } else if (normalized.compareTo(expected) < 0) {
            item.setStatus(MonthlyPlanItemStatus.PARTIALLY_PAID);
        } else {
            item.setStatus(MonthlyPlanItemStatus.PAID);
        }
    }

    private MonthlyPlanItem ensurePayable(String ownerEmail, Long id) {
        MonthlyPlanItem item = financialPeriodService.findOwnedPlanItem(ownerEmail, id);
        if (item.getType() != TransactionType.EXPENSE || item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            throw new ResourceNotFoundException("Conta mensal nao encontrada.");
        }
        return item;
    }

    private MonthlyPlanItem ensureIncomePlan(String ownerEmail, Long id) {
        MonthlyPlanItem item = financialPeriodService.findOwnedPlanItem(ownerEmail, id);
        if (item.getType() != TransactionType.INCOME || item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            throw new ResourceNotFoundException("Receita mensal nao encontrada.");
        }
        return item;
    }

    private void ensureCanReceivePayment(MonthlyPlanItem item) {
        if (item.getFinancialPeriod().getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Este ciclo mensal esta fechado. Reabra o ciclo antes de registrar pagamentos.");
        }
        if (item.getStatus() == MonthlyPlanItemStatus.CANCELED) {
            throw new BusinessException("Nao e possivel registrar pagamento em item cancelado.");
        }
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            throw new BusinessException("Itens internos de fatura nao recebem pagamento individual.");
        }
    }

    private Account resolveAccount(String ownerEmail, MonthlyPlanItem item, Long accountId) {
        if (accountId != null) {
            return accountService.findOwnedAccount(ownerEmail, accountId);
        }
        if (item.getAccount() != null) {
            return item.getAccount();
        }
        return accountService.getOrCreateDefaultAccount(ownerEmail);
    }

    private BigDecimal remainingOrExpected(MonthlyPlanItem item) {
        BigDecimal remaining = nullToZero(item.getExpectedAmount()).subtract(nullToZero(item.getActualAmount()));
        if (remaining.signum() > 0) {
            return remaining;
        }
        return nullToZero(item.getExpectedAmount());
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("O valor deve ser maior que zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void applySavingsJarContribution(String ownerEmail, MonthlyPlanItem item, Payment payment, boolean reversing) {
        if (item == null || item.getNature() != MonthlyPlanItemNature.SAVINGS_JAR || item.getType() != TransactionType.EXPENSE) {
            return;
        }
        Long jarId = SavingsJarContributionPlanService.parseJarId(item.getNotes());
        if (jarId == null) {
            return;
        }
        SavingsJarMovementRequest request = new SavingsJarMovementRequest(
                payment.getAmount(),
                payment.getPaymentDate(),
                reversing ? "Estorno de aporte planejado" : "Aporte planejado do mes",
                payment.getSource() == PaymentSource.AI_CHAT ? TransactionSource.AI_CHAT : TransactionSource.MANUAL,
                "Pagamento #" + payment.getId() + " da obrigacao mensal #" + item.getId()
        );
        if (reversing) {
            savingsJarService.withdraw(ownerEmail, jarId, request);
        } else {
            savingsJarService.deposit(ownerEmail, jarId, request);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String appendNote(String note, String extra) {
        String normalizedExtra = normalizeNullable(extra);
        if (normalizedExtra == null) {
            return normalizeNullable(note);
        }
        String normalizedNote = normalizeNullable(note);
        return normalizedNote == null ? normalizedExtra : normalizedNote + "\n" + normalizedExtra;
    }
}
