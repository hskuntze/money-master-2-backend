package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodTurnoverRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemInvoiceLinkRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentSource;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemInvoiceContributionMode;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialPeriodRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseEntryRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;

@Service
public class FinancialPeriodService {

    private static final DateTimeFormatter DEFAULT_NAME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinancialPeriodRepository periodRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final InstallmentPurchaseEntryRepository installmentEntryRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final MonthlyPeriodSummaryCalculator summaryCalculator;

    public FinancialPeriodService(
            FinancialPeriodRepository periodRepository,
            MonthlyPlanItemRepository planItemRepository,
            FinancialTransactionRepository transactionRepository,
            InstallmentPurchaseEntryRepository installmentEntryRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            MonthlyPeriodSummaryCalculator summaryCalculator
    ) {
        this.periodRepository = periodRepository;
        this.planItemRepository = planItemRepository;
        this.transactionRepository = transactionRepository;
        this.installmentEntryRepository = installmentEntryRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.summaryCalculator = summaryCalculator;
    }

    @Transactional(readOnly = true)
    public List<FinancialPeriodResponse> list(String ownerEmail) {
        return periodRepository.findByOwnerEmailOrderByStartDateDesc(ownerEmail)
                .stream()
                .map(FinancialPeriodResponse::from)
                .toList();
    }

    @Transactional
    public FinancialPeriodResponse current(String ownerEmail) {
        return FinancialPeriodResponse.from(findOrCreateForDate(ownerEmail, LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public FinancialPeriodResponse get(String ownerEmail, Long id) {
        return FinancialPeriodResponse.from(findOwnedPeriod(ownerEmail, id));
    }

    /**
     * Faz a virada quando a data informada já chegou, ou agenda o próximo ciclo
     * quando a data informada ainda está no futuro.
     *
     * Exemplo: em 23/05, informar 04/06 não deve fechar maio imediatamente.
     * O ciclo atual passa a terminar em 03/06 e o ciclo de 04/06 nasce como SCHEDULED.
     */
    @Transactional
    public FinancialPeriodResponse turnover(String ownerEmail, FinancialPeriodTurnoverRequest request) {
        LocalDate turnoverDate = request.turnoverDate();
        if (turnoverDate == null) {
            throw new BusinessException("A data da virada é obrigatória.");
        }

        LocalDate today = LocalDate.now();
        FinancialPeriod currentPeriod = periodRepository.findPeriodContainingDate(ownerEmail, today)
                .orElseGet(() -> findOrCreateForDate(ownerEmail, today));

        if (!turnoverDate.isAfter(currentPeriod.getStartDate())) {
            throw new BusinessException("A data da virada deve ser posterior ao início do ciclo financeiro atual.");
        }

        LocalDate previousEnd = turnoverDate.minusDays(1);
        if (previousEnd.isBefore(currentPeriod.getStartDate())) {
            throw new BusinessException("A data final do ciclo atual ficou inválida.");
        }

        currentPeriod.setEndDate(previousEnd);
        currentPeriod.setTurnoverDay(turnoverDate.getDayOfMonth());

        User owner = currentUserService.findUserByEmail(ownerEmail);
        FinancialPeriod nextPeriod = periodRepository.findByOwnerEmailAndStartDate(ownerEmail, turnoverDate)
                .orElseGet(() -> new FinancialPeriod());

        boolean newNextPeriod = nextPeriod.getId() == null;
        nextPeriod.setOwner(owner);
        nextPeriod.setName(normalizeNullableOrDefault(request.newPeriodName(), defaultPeriodName(turnoverDate)));
        nextPeriod.setStartDate(turnoverDate);
        nextPeriod.setEndDate(turnoverDate.plusMonths(1).minusDays(1));
        nextPeriod.setTurnoverDay(turnoverDate.getDayOfMonth());

        if (turnoverDate.isAfter(today)) {
            currentPeriod.setStatus(FinancialPeriodStatus.OPEN);
            currentPeriod.setClosedAt(null);
            nextPeriod.setStatus(FinancialPeriodStatus.SCHEDULED);
            nextPeriod.setClosedAt(null);
            FinancialPeriod savedNext = periodRepository.save(nextPeriod);
            if (newNextPeriod) {
                cloneRecurringPlanItems(ownerEmail, currentPeriod, savedNext);
            }
            return FinancialPeriodResponse.from(savedNext);
        }

        validateCanClose(currentPeriod);
        closePeriod(ownerEmail, currentPeriod, previousEnd);
        nextPeriod.setStatus(FinancialPeriodStatus.OPEN);
        nextPeriod.setClosedAt(null);
        demoteOtherOpenPeriods(ownerEmail, nextPeriod.getId());
        FinancialPeriod savedNext = periodRepository.save(nextPeriod);
        if (newNextPeriod) {
            cloneRecurringPlanItems(ownerEmail, currentPeriod, savedNext);
        }
        return FinancialPeriodResponse.from(savedNext);
    }

    @Transactional
    public FinancialPeriodResponse updatePeriod(String ownerEmail, Long id, FinancialPeriodUpdateRequest request) {
        FinancialPeriod period = findOwnedPeriod(ownerEmail, id);
        LocalDate startDate = request.startDate() == null ? period.getStartDate() : request.startDate();
        LocalDate endDate = request.endDate() == null ? period.getEndDate() : request.endDate();
        validatePeriodRange(startDate, endDate);
        validateNoOverlap(ownerEmail, period.getId(), startDate, endDate);

        if (request.name() != null) {
            period.setName(normalizeNullableOrDefault(request.name(), defaultPeriodName(startDate)));
        }
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        if (request.turnoverDay() != null) {
            period.setTurnoverDay(normalizeTurnoverDay(request.turnoverDay()));
        }
        if (request.status() != null) {
            applyStatus(ownerEmail, period, request.status());
        }
        return FinancialPeriodResponse.from(period);
    }

    @Transactional
    public FinancialPeriodResponse reopenPeriod(String ownerEmail, Long id) {
        FinancialPeriod period = findOwnedPeriod(ownerEmail, id);
        applyStatus(ownerEmail, period, FinancialPeriodStatus.OPEN);
        return FinancialPeriodResponse.from(period);
    }

    @Transactional
    public FinancialPeriodResponse closePeriodById(String ownerEmail, Long id) {
        FinancialPeriod period = findOwnedPeriod(ownerEmail, id);
        applyStatus(ownerEmail, period, FinancialPeriodStatus.CLOSED);
        FinancialPeriod nextPeriod = ensureNextPeriodAfterClose(ownerEmail, period);
        return FinancialPeriodResponse.from(nextPeriod);
    }

    @Transactional(readOnly = true)
    public MonthlyPeriodSummaryResponse summary(String ownerEmail, Long periodId) {
        FinancialPeriod period = findOwnedPeriod(ownerEmail, periodId);
        List<MonthlyPlanItem> items = planItemRepository.findByOwnerEmailAndPeriod(ownerEmail, period.getId(), null);
        List<FinancialTransaction> transactions = transactionRepository.search(
                ownerEmail,
                period.getStartDate(),
                period.getEndDate(),
                null,
                null,
                null,
                period.getId(),
                null
        );
        return summaryCalculator.calculate(period, items, transactions);
    }

    @Transactional(readOnly = true)
    public List<MonthlyPlanItemResponse> listPlanItems(String ownerEmail, Long periodId, MonthlyPlanItemStatus status) {
        findOwnedPeriod(ownerEmail, periodId);
        List<MonthlyPlanItem> items = planItemRepository.findByOwnerEmailAndPeriod(ownerEmail, periodId, null);
        return toTreeResponses(items, status);
    }

    @Transactional(readOnly = true)
    public List<MonthlyPlanItemResponse> listInvoiceChildCandidates(String ownerEmail, Long invoiceItemId) {
        MonthlyPlanItem invoice = findOwnedPlanItem(ownerEmail, invoiceItemId);
        validateInvoiceParent(invoice);
        return planItemRepository.findInvoiceChildCandidates(ownerEmail, invoice.getFinancialPeriod().getId(), invoice.getId())
                .stream()
                .map(MonthlyPlanItemResponse::from)
                .toList();
    }

    @Transactional
    public MonthlyPlanItemResponse createPlanItem(String ownerEmail, Long periodId, MonthlyPlanItemCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        FinancialPeriod period = findOwnedPeriod(ownerEmail, periodId);
        ensurePlanningEditable(period);
        validateDueDate(period, request.dueDate());

        Account account = request.accountId() == null ? accountService.getOrCreateDefaultAccount(ownerEmail) : accountService.findOwnedAccount(ownerEmail, request.accountId());
        Category category = request.categoryId() == null ? null : categoryService.findAvailableCategory(ownerEmail, request.categoryId());
        validateCategoryType(category, request.type());

        MonthlyPlanItemAggregationType aggregationType = resolveAggregationType(request.aggregationType(), request.parentItemId());
        MonthlyPlanItem parent = null;
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            if (request.parentItemId() == null) {
                throw new BusinessException("Selecione a fatura vinculada para criar um item interno.");
            }
            parent = findOwnedPlanItem(ownerEmail, request.parentItemId());
        }

        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setOwner(owner);
        item.setFinancialPeriod(period);
        item.setAccount(account);
        item.setCategory(category);
        item.setType(request.type());
        item.setDescription(normalizeRequired(request.description(), "A descrição é obrigatória."));
        item.setExpectedAmount(normalizeZeroOrPositive(request.expectedAmount()));
        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setDueDate(request.dueDate());
        item.setNature(resolveNature(request.nature(), aggregationType));
        item.setAggregationType(aggregationType);
        item.setInvoiceContributionMode(resolveInvoiceContributionMode(request.invoiceContributionMode(), aggregationType));
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            if (item.getType() != TransactionType.EXPENSE) {
                throw new BusinessException("A fatura manual precisa ser um item de despesa.");
            }
            item.setInvoiceBaseAmount(item.getExpectedAmount());
        } else {
            item.setInvoiceBaseAmount(null);
        }
        item.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        item.setPaidByParent(false);
        if (parent != null) {
            validateParentChildRelationship(parent, item);
            item.setParentItem(parent);
            item.setAggregationType(MonthlyPlanItemAggregationType.GROUP_CHILD);
            item.setInvoiceContributionMode(resolveInvoiceContributionMode(request.invoiceContributionMode(), MonthlyPlanItemAggregationType.GROUP_CHILD));
        }
        item.setRecurring(item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD ? false : Boolean.TRUE.equals(request.recurring()));
        item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), request.recurrenceEndDate(), item.getDueDate()));
        item.setStatus(request.status() == null ? MonthlyPlanItemStatus.PENDING : request.status());
        item.setNotes(normalizeNullable(request.notes()));
        rejectDirectSettlementStatus(item.getStatus(), null);

        MonthlyPlanItem saved = planItemRepository.save(item);
        initializeRecurringMetadata(saved);
        if (parent != null) {
            synchronizeInvoiceTotal(parent);
            synchronizeChildrenWithParentPayment(parent);
        }
        propagateRecurringPlanItemToExistingFuturePeriods(ownerEmail, saved);
        return MonthlyPlanItemResponse.from(saved);
    }

    @Transactional
    public MonthlyPlanItemResponse updatePlanItem(String ownerEmail, Long itemId, MonthlyPlanItemUpdateRequest request) {
        MonthlyPlanItem item = findOwnedPlanItem(ownerEmail, itemId);
        ensurePlanningEditable(item.getFinancialPeriod());

        MonthlyPlanItem previousParent = item.getParentItem();
        MonthlyPlanItemAggregationType targetAggregationType = resolveTargetAggregationType(item, request.aggregationType(), request.parentItemId());
        boolean generatedOccurrence = isGeneratedRecurringOccurrence(item);
        TransactionType targetType = request.type() == null ? item.getType() : request.type();
        if (request.accountId() != null) {
            item.setAccount(accountService.findOwnedAccount(ownerEmail, request.accountId()));
        }
        if (request.categoryId() != null) {
            Category category = categoryService.findAvailableCategory(ownerEmail, request.categoryId());
            validateCategoryType(category, targetType);
            item.setCategory(category);
        }
        if (request.type() != null) {
            validateCategoryType(item.getCategory(), request.type());
            item.setType(request.type());
        }
        if (request.description() != null && !request.description().isBlank()) {
            item.setDescription(request.description().trim());
        }
        if (request.expectedAmount() != null) {
            BigDecimal normalizedExpected = normalizeZeroOrPositive(request.expectedAmount());
            if (targetAggregationType == MonthlyPlanItemAggregationType.GROUP_PARENT) {
                item.setInvoiceBaseAmount(normalizedExpected);
            } else {
                item.setExpectedAmount(normalizedExpected);
            }
        }
        if (request.actualAmount() != null) {
            throw new BusinessException("O valor realizado nao pode ser editado diretamente. Registre uma baixa, recebimento ou conciliacao para alterar este valor.");
        }
        if (request.dueDate() != null) {
            validateDueDate(item.getFinancialPeriod(), request.dueDate());
            item.setDueDate(request.dueDate());
            item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), item.getRecurrenceEndDate(), item.getDueDate()));
        }
        if (request.paidOn() != null && !Objects.equals(request.paidOn(), item.getPaidOn())) {
            throw new BusinessException("A data de pagamento ou recebimento deve vir da baixa, recebimento ou conciliacao vinculada.");
        }
        if (request.nature() != null) {
            item.setNature(request.nature());
        }

        applyRequestedHierarchy(ownerEmail, item, request.aggregationType(), request.parentItemId(), request.invoiceContributionMode());
        applyInvoiceFieldsAfterHierarchy(item, request.invoiceContributionMode());

        if (request.recurring() != null) {
            item.setRecurring(item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD ? false : request.recurring());
            if (!item.isRecurring()) {
                item.setRecurrenceEndDate(null);
                item.setRecurringTemplateId(null);
                item.setGeneratedFromItemId(null);
                item.setRecurrenceKey(null);
            }
        }
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            item.setRecurring(false);
            item.setRecurrenceEndDate(null);
            item.setRecurringTemplateId(null);
            item.setGeneratedFromItemId(null);
            item.setRecurrenceKey(null);
        }
        if (request.recurrenceEndDate() != null || request.recurring() != null) {
            item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), request.recurrenceEndDate(), item.getDueDate()));
        }
        if (request.status() != null) {
            rejectDirectSettlementStatus(request.status(), item.getStatus());
            item.setStatus(request.status());
        }
        if (request.notes() != null) {
            item.setNotes(normalizeNullable(request.notes()));
        }
        if (generatedOccurrence) {
            item.setRecurrenceModifiedManually(true);
        }
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            synchronizeInvoiceTotal(item);
            synchronizeChildrenWithParentPayment(item);
        }
        if (previousParent != null && !Objects.equals(previousParent.getId(), item.getParentItem() == null ? null : item.getParentItem().getId())) {
            synchronizeInvoiceTotal(previousParent);
            synchronizeChildrenWithParentPayment(previousParent);
        }
        if (item.getParentItem() != null) {
            synchronizeInvoiceTotal(item.getParentItem());
            synchronizeChildrenWithParentPayment(item.getParentItem());
        }
        recalculatePlanItemStatus(item);
        initializeRecurringMetadata(item);
        propagateRecurringPlanItemToExistingFuturePeriods(ownerEmail, item);
        return MonthlyPlanItemResponse.from(item);
    }

    @Transactional
    public void cancelPlanItem(String ownerEmail, Long itemId) {
        MonthlyPlanItem item = findOwnedPlanItem(ownerEmail, itemId);
        ensurePlanningEditable(item.getFinancialPeriod());
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            List<MonthlyPlanItem> children = planItemRepository.findChildrenByParentIdAndOwnerEmail(item.getId(), ownerEmail);
            if (!children.isEmpty()) {
                throw new BusinessException("Desvincule os itens internos antes de cancelar a fatura.");
            }
        }
        item.setStatus(MonthlyPlanItemStatus.CANCELED);
        if (item.getParentItem() != null) {
            synchronizeInvoiceTotal(item.getParentItem());
            synchronizeChildrenWithParentPayment(item.getParentItem());
        }
    }

    @Transactional
    public MonthlyPlanItemResponse linkPlanItemToInvoice(String ownerEmail, Long invoiceItemId, Long childItemId, MonthlyPlanItemInvoiceLinkRequest request) {
        MonthlyPlanItem invoice = findOwnedPlanItem(ownerEmail, invoiceItemId);
        MonthlyPlanItem child = findOwnedPlanItem(ownerEmail, childItemId);
        MonthlyPlanItem previousParent = child.getParentItem();
        ensurePlanningEditable(invoice.getFinancialPeriod());
        if (previousParent != null) {
            ensurePlanningEditable(previousParent.getFinancialPeriod());
        }
        linkChildToParent(invoice, child, request == null ? null : request.invoiceContributionMode());
        if (previousParent != null && !Objects.equals(previousParent.getId(), invoice.getId())) {
            synchronizeInvoiceTotal(previousParent);
            synchronizeChildrenWithParentPayment(previousParent);
        }
        synchronizeInvoiceTotal(invoice);
        synchronizeChildrenWithParentPayment(invoice);
        return toSingleTreeResponse(ownerEmail, invoice.getId());
    }

    @Transactional
    public MonthlyPlanItemResponse unlinkPlanItemFromInvoice(String ownerEmail, Long childItemId) {
        MonthlyPlanItem child = findOwnedPlanItem(ownerEmail, childItemId);
        ensurePlanningEditable(child.getFinancialPeriod());
        MonthlyPlanItem parent = child.getParentItem();
        if (parent == null) {
            throw new BusinessException("Este item não está vinculado a uma fatura.");
        }
        child.setParentItem(null);
        child.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        child.setInvoiceContributionMode(MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY);
        child.setInvoiceBaseAmount(null);
        if (child.isPaidByParent()) {
            child.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            child.setPaidOn(null);
            child.setStatus(MonthlyPlanItemStatus.PENDING);
        }
        child.setPaidByParent(false);
        child.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        synchronizeInvoiceTotal(parent);
        synchronizeChildrenWithParentPayment(parent);
        return MonthlyPlanItemResponse.from(child);
    }

    @Transactional(readOnly = true)
    public FinancialPeriod findOwnedPeriod(String ownerEmail, Long id) {
        return periodRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Mês financeiro não encontrado."));
    }

    @Transactional(readOnly = true)
    public MonthlyPlanItem findOwnedPlanItem(String ownerEmail, Long id) {
        return planItemRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Conta planejada do mês não encontrada."));
    }

    @Transactional
    public FinancialPeriod findOrCreateForDate(String ownerEmail, LocalDate date) {
        LocalDate reference = date == null ? LocalDate.now() : date;
        return periodRepository.findPeriodContainingDate(ownerEmail, reference)
                .orElseGet(() -> createDefaultPeriod(ownerEmail, reference));
    }

    @Transactional
    public List<MonthlyPlanItemResponse> createInstallmentPlanItemsFromAi(
            String ownerEmail,
            String typeText,
            String planItemDescription,
            BigDecimal installmentAmount,
            Integer installmentCount,
            String firstDueDateText,
            String accountName,
            String categoryName,
            String natureText,
            Boolean recurring,
            String recurrenceEndDateText,
            String notes
    ) {
        int count = installmentCount == null || installmentCount <= 0 ? 1 : installmentCount;
        if (count > 120) {
            throw new BusinessException("A quantidade de parcelas não pode ultrapassar 120 meses.");
        }
        LocalDate firstDueDate = parseDateOrToday(firstDueDateText);
        List<MonthlyPlanItemResponse> responses = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            LocalDate dueDate = firstDueDate.plusMonths(index);
            String installmentNote = appendNote(notes, count > 1 ? "Parcela " + (index + 1) + " de " + count + "." : null);
            responses.add(increaseMonthlyPlanItemExpectedAmountFromAi(
                    ownerEmail,
                    typeText,
                    planItemDescription,
                    installmentAmount,
                    dueDate.toString(),
                    accountName,
                    categoryName,
                    natureText,
                    recurring,
                    recurrenceEndDateText,
                    installmentNote
            ));
        }
        return responses;
    }

    @Transactional
    public MonthlyPlanItemResponse increaseMonthlyPlanItemExpectedAmountFromAi(
            String ownerEmail,
            String typeText,
            String planItemDescription,
            BigDecimal amountToAdd,
            String dueDateText,
            String accountName,
            String categoryName,
            String natureText,
            Boolean recurring,
            String recurrenceEndDateText,
            String notes
    ) {
        TransactionType type = parseTransactionType(typeText);
        BigDecimal amount = normalizeZeroOrPositive(amountToAdd);
        LocalDate dueDate = parseDateOrToday(dueDateText);
        FinancialPeriod period = findOrCreateForDate(ownerEmail, dueDate);
        ensurePlanningEditable(period);
        if (dueDate.isBefore(period.getStartDate()) || dueDate.isAfter(period.getEndDate())) {
            dueDate = clampDate(dueDate, period);
        }
        Account account = accountService.resolveForAi(ownerEmail, null, accountName);
        Category category = categoryName == null || categoryName.isBlank() ? null : categoryService.resolveForAi(ownerEmail, null, categoryName, type);
        MonthlyPlanItem item = resolveOrCreateVariablePlanItem(ownerEmail, period, type, planItemDescription, dueDate, account, category, natureText, Boolean.TRUE.equals(recurring), recurrenceEndDateText, notes);
        item.setExpectedAmount(nullToZero(item.getExpectedAmount()).add(amount).setScale(2, RoundingMode.HALF_UP));
        if (account != null && item.getAccount() == null) {
            item.setAccount(account);
        }
        if (category != null && item.getCategory() == null) {
            item.setCategory(category);
        }
        if (recurring != null) {
            item.setRecurring(recurring);
        }
        if (recurrenceEndDateText != null && !recurrenceEndDateText.isBlank()) {
            item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), parseDateOrNull(recurrenceEndDateText), item.getDueDate()));
        }
        if (notes != null && !notes.isBlank()) {
            item.setNotes(appendNote(item.getNotes(), notes));
        }
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            synchronizeInvoiceTotal(item);
        }
        if (item.getParentItem() != null) {
            synchronizeInvoiceTotal(item.getParentItem());
            synchronizeChildrenWithParentPayment(item.getParentItem());
        }
        recalculatePlanItemStatus(item);
        return MonthlyPlanItemResponse.from(planItemRepository.save(item));
    }

    @Transactional
    public void registerPaymentForPlanItem(MonthlyPlanItem item, FinancialTransaction transaction) {
        synchronizePlanItemPayment(item);
    }

    @Transactional
    public void synchronizePlanItemPayment(MonthlyPlanItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        BigDecimal paymentTotal = item.getType() == TransactionType.INCOME
                ? paymentRepository.sumActiveByIncomePlan(item.getId())
                : paymentRepository.sumActiveByPayable(item.getId());
        if (paymentTotal != null && paymentTotal.signum() > 0) {
            item.setActualAmount(nullToZero(paymentTotal));
            item.setPaidOn(paymentRepository.findLatestActivePaymentDateByPlanItem(item.getId()));
        } else {
            BigDecimal actual = transactionRepository.sumAmountByMonthlyPlanItem(item.getId());
            item.setActualAmount(nullToZero(actual));
            item.setPaidOn(transactionRepository.findLatestPaymentDateByMonthlyPlanItem(item.getId()));
        }
        recalculatePlanItemStatus(item);
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            synchronizeChildrenWithParentPayment(item);
        }
    }

    private FinancialPeriod createDefaultPeriod(String ownerEmail, LocalDate reference) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        FinancialPeriod sourcePeriod = periodRepository.findLatestBeforeDate(ownerEmail, reference)
                .stream()
                .findFirst()
                .orElse(null);

        LocalDate start = calculateDefaultStart(reference, sourcePeriod);
        LocalDate end = calculateDefaultEnd(start, sourcePeriod);

        periodRepository.findOpenPeriods(ownerEmail)
                .stream()
                .filter(openPeriod -> openPeriod.getEndDate().isBefore(start))
                .forEach(openPeriod -> closePeriod(ownerEmail, openPeriod, openPeriod.getEndDate()));

        FinancialPeriod period = new FinancialPeriod();
        period.setOwner(owner);
        period.setName(defaultPeriodName(start));
        period.setStartDate(start);
        period.setEndDate(end);
        period.setTurnoverDay(start.getDayOfMonth());
        period.setStatus(start.isAfter(LocalDate.now()) ? FinancialPeriodStatus.SCHEDULED : FinancialPeriodStatus.OPEN);
        FinancialPeriod saved = periodRepository.save(period);
        if (sourcePeriod != null) {
            cloneRecurringPlanItems(ownerEmail, sourcePeriod, saved);
        }
        return saved;
    }

    private LocalDate calculateDefaultStart(LocalDate reference, FinancialPeriod sourcePeriod) {
        if (sourcePeriod == null) {
            return reference.withDayOfMonth(1);
        }
        LocalDate start = sourcePeriod.getEndDate().plusDays(1);
        LocalDate end = calculateDefaultEnd(start, sourcePeriod);
        while (reference.isAfter(end)) {
            start = end.plusDays(1);
            end = calculateDefaultEnd(start, sourcePeriod);
        }
        return start;
    }

    private LocalDate calculateDefaultEnd(LocalDate start, FinancialPeriod sourcePeriod) {
        return start.plusMonths(1).minusDays(1);
    }

    private MonthlyPlanItem resolveOrCreateVariablePlanItem(
            String ownerEmail,
            FinancialPeriod period,
            TransactionType type,
            String description,
            LocalDate dueDate,
            Account account,
            Category category,
            String natureText,
            boolean recurring,
            String recurrenceEndDateText,
            String notes
    ) {
        String safeDescription = normalizeRequired(description, "Informe a descrição do item planejado.");
        List<MonthlyPlanItem> matches = planItemRepository.findByDescriptionContainingInPeriod(ownerEmail, period.getId(), type, safeDescription);
        if (matches.isEmpty() && category != null) {
            matches = planItemRepository.findActiveCandidatesByOwnerEmailAndPeriod(ownerEmail, period.getId(), type)
                    .stream()
                    .filter(item -> item.getCategory() != null && item.getCategory().getId().equals(category.getId()))
                    .toList();
        }
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setOwner(currentUserService.findUserByEmail(ownerEmail));
        item.setFinancialPeriod(period);
        item.setAccount(account == null ? accountService.getOrCreateDefaultAccount(ownerEmail) : account);
        item.setCategory(category);
        item.setType(type);
        item.setDescription(safeDescription);
        item.setExpectedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setDueDate(dueDate);
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(parseNatureOrDefault(natureText));
        item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        item.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        item.setPaidByParent(false);
        item.setRecurring(recurring);
        item.setRecurrenceEndDate(validateRecurrenceEndDate(recurring, parseDateOrNull(recurrenceEndDateText), dueDate));
        item.setNotes(normalizeNullable(notes));
        return planItemRepository.save(item);
    }


    private FinancialPeriod ensureNextPeriodAfterClose(String ownerEmail, FinancialPeriod closedPeriod) {
        LocalDate nextStart = closedPeriod.getEndDate().plusDays(1);
        FinancialPeriod nextPeriod = periodRepository.findByOwnerEmailAndStartDate(ownerEmail, nextStart)
                .orElseGet(() -> {
                    FinancialPeriod created = new FinancialPeriod();
                    created.setOwner(closedPeriod.getOwner());
                    created.setName(defaultPeriodName(nextStart));
                    created.setStartDate(nextStart);
                    created.setEndDate(nextStart.plusMonths(1).minusDays(1));
                    created.setTurnoverDay(nextStart.getDayOfMonth());
                    created.setStatus(nextStart.isAfter(LocalDate.now()) ? FinancialPeriodStatus.SCHEDULED : FinancialPeriodStatus.OPEN);
                    return periodRepository.save(created);
                });
        cloneRecurringPlanItems(ownerEmail, closedPeriod, nextPeriod);
        if (nextPeriod.getStatus() != FinancialPeriodStatus.OPEN && !nextPeriod.getStartDate().isAfter(LocalDate.now())) {
            nextPeriod.setStatus(FinancialPeriodStatus.OPEN);
            nextPeriod.setClosedAt(null);
        }
        if (nextPeriod.getStatus() == FinancialPeriodStatus.OPEN) {
            demoteOtherOpenPeriods(ownerEmail, nextPeriod.getId());
        }
        return nextPeriod;
    }

    private void closePeriod(String ownerEmail, FinancialPeriod period, LocalDate endDate) {
        period.setEndDate(endDate);
        BigDecimal income = transactionRepository.sumAmount(ownerEmail, null, period.getStartDate(), period.getEndDate(), period.getId(), TransactionType.INCOME);
        BigDecimal expense = transactionRepository.sumAmount(ownerEmail, null, period.getStartDate(), period.getEndDate(), period.getId(), TransactionType.EXPENSE);
        BigDecimal transfer = transactionRepository.sumAmount(ownerEmail, null, period.getStartDate(), period.getEndDate(), period.getId(), TransactionType.TRANSFER);
        period.setArchivedIncomeTotal(nullToZero(income));
        period.setArchivedExpenseTotal(nullToZero(expense));
        period.setArchivedTransferTotal(nullToZero(transfer));
        period.setArchivedNetTotal(nullToZero(income).subtract(nullToZero(expense)).setScale(2, RoundingMode.HALF_UP));
        period.setStatus(FinancialPeriodStatus.CLOSED);
        period.setClosedAt(Instant.now());
    }

    private void cloneRecurringPlanItems(String ownerEmail, FinancialPeriod sourcePeriod, FinancialPeriod targetPeriod) {
        List<MonthlyPlanItem> recurringItems = planItemRepository.findRecurringByOwnerEmailAndPeriod(ownerEmail, sourcePeriod.getId());
        recurringItems.forEach(source -> upsertRecurringOccurrence(ownerEmail, source, targetPeriod));
    }

    private void propagateRecurringPlanItemToExistingFuturePeriods(String ownerEmail, MonthlyPlanItem source) {
        if (source == null || source.getId() == null || !source.isRecurring()) {
            return;
        }
        if (source.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD || source.getParentItem() != null) {
            return;
        }
        initializeRecurringMetadata(source);
        List<FinancialPeriod> futurePeriods = periodRepository.findFuturePeriodsAfterStartDate(ownerEmail, source.getFinancialPeriod().getStartDate());
        for (FinancialPeriod targetPeriod : futurePeriods) {
            if (targetPeriod.getStatus() == FinancialPeriodStatus.CLOSED) {
                continue;
            }
            upsertRecurringOccurrence(ownerEmail, source, targetPeriod);
        }
    }

    private void upsertRecurringOccurrence(String ownerEmail, MonthlyPlanItem source, FinancialPeriod targetPeriod) {
        if (source == null || source.getId() == null || targetPeriod == null || targetPeriod.getId() == null) {
            return;
        }
        if (!source.isRecurring() || source.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD || source.getParentItem() != null) {
            return;
        }
        if (Objects.equals(source.getFinancialPeriod().getId(), targetPeriod.getId())) {
            return;
        }
        if (targetPeriod.getStatus() == FinancialPeriodStatus.CLOSED) {
            return;
        }
        initializeRecurringMetadata(source);
        Long templateId = recurrenceTemplateId(source);
        LocalDate dueDate = copyDayIntoPeriod(source.getDueDate(), targetPeriod);
        if (source.getRecurrenceEndDate() != null && dueDate.isAfter(source.getRecurrenceEndDate())) {
            return;
        }

        String key = recurrenceKey(templateId, targetPeriod.getId());
        MonthlyPlanItem target = planItemRepository.findByOwnerEmailAndRecurrenceKey(ownerEmail, key).orElse(null);
        if (target == null) {
            target = findLegacyRecurringOccurrenceCandidate(ownerEmail, source, targetPeriod);
        }
        if (target == null) {
            target = new MonthlyPlanItem();
            target.setOwner(source.getOwner());
            target.setFinancialPeriod(targetPeriod);
            target.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            target.setStatus(MonthlyPlanItemStatus.PENDING);
            target.setPaidOn(null);
            target.setRecurring(true);
            target.setRecurrenceModifiedManually(false);
        } else if (!canUpdateGeneratedRecurringOccurrence(target)) {
            return;
        }
        target.setRecurringTemplateId(templateId);
        target.setGeneratedFromItemId(source.getId());
        target.setRecurrenceKey(key);

        target.setAccount(source.getAccount());
        target.setCategory(source.getCategory());
        target.setType(source.getType());
        target.setDescription(source.getDescription());
        target.setExpectedAmount(source.getExpectedAmount());
        target.setDueDate(dueDate);
        target.setNature(source.getNature());
        target.setAggregationType(source.getAggregationType() == null ? MonthlyPlanItemAggregationType.NORMAL : source.getAggregationType());
        target.setParentItem(null);
        target.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        target.setPaidByParent(false);
        target.setRecurring(true);
        target.setRecurrenceEndDate(source.getRecurrenceEndDate());
        target.setNotes(source.getNotes());
        planItemRepository.save(target);
    }


    private MonthlyPlanItem findLegacyRecurringOccurrenceCandidate(String ownerEmail, MonthlyPlanItem source, FinancialPeriod targetPeriod) {
        return planItemRepository.findByOwnerEmailAndPeriod(ownerEmail, targetPeriod.getId(), null)
                .stream()
                .filter(candidate -> candidate.isRecurring())
                .filter(candidate -> candidate.getParentItem() == null)
                .filter(candidate -> candidate.getAggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD)
                .filter(candidate -> candidate.getStatus() != MonthlyPlanItemStatus.CANCELED)
                .filter(candidate -> candidate.getType() == source.getType())
                .filter(candidate -> normalizeComparable(candidate.getDescription()).equals(normalizeComparable(source.getDescription())))
                .filter(this::canUpdateGeneratedRecurringOccurrence)
                .findFirst()
                .orElse(null);
    }

    private boolean canUpdateGeneratedRecurringOccurrence(MonthlyPlanItem item) {
        return item != null
                && !item.isRecurrenceModifiedManually()
                && item.getFinancialPeriod().getStatus() != FinancialPeriodStatus.CLOSED
                && item.getStatus() == MonthlyPlanItemStatus.PENDING
                && nullToZero(item.getActualAmount()).signum() == 0;
    }

    private void initializeRecurringMetadata(MonthlyPlanItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        if (!item.isRecurring()) {
            return;
        }
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD || item.getParentItem() != null) {
            item.setRecurring(false);
            item.setRecurrenceEndDate(null);
            item.setRecurringTemplateId(null);
            item.setGeneratedFromItemId(null);
            item.setRecurrenceKey(null);
            return;
        }
        Long templateId = recurrenceTemplateId(item);
        item.setRecurringTemplateId(templateId);
        if (item.getRecurrenceKey() == null || item.getRecurrenceKey().isBlank()) {
            item.setRecurrenceKey(recurrenceKey(templateId, item.getFinancialPeriod().getId()));
        }
    }

    private Long recurrenceTemplateId(MonthlyPlanItem item) {
        return item.getRecurringTemplateId() == null ? item.getId() : item.getRecurringTemplateId();
    }

    private boolean isGeneratedRecurringOccurrence(MonthlyPlanItem item) {
        return item != null
                && item.getId() != null
                && item.getRecurringTemplateId() != null
                && !Objects.equals(item.getRecurringTemplateId(), item.getId());
    }

    private String recurrenceKey(Long templateId, Long periodId) {
        return "monthly-plan-item:" + templateId + ":period:" + periodId;
    }

    private LocalDate copyDayIntoPeriod(LocalDate sourceDate, FinancialPeriod targetPeriod) {
        int day = Math.min(sourceDate.getDayOfMonth(), targetPeriod.getStartDate().lengthOfMonth());
        LocalDate candidate = targetPeriod.getStartDate().withDayOfMonth(day);
        if (candidate.isBefore(targetPeriod.getStartDate())) {
            candidate = candidate.plusMonths(1);
        }
        if (candidate.isAfter(targetPeriod.getEndDate())) {
            candidate = targetPeriod.getEndDate();
        }
        return candidate;
    }

    private void applyStatus(String ownerEmail, FinancialPeriod period, FinancialPeriodStatus status) {
        if (status == FinancialPeriodStatus.CLOSED) {
            validateCanClose(period);
            closePeriod(ownerEmail, period, period.getEndDate());
            return;
        }
        period.setStatus(status);
        period.setClosedAt(null);
        if (status == FinancialPeriodStatus.OPEN) {
            demoteOtherOpenPeriods(ownerEmail, period.getId());
        }
    }

    private void demoteOtherOpenPeriods(String ownerEmail, Long keepOpenPeriodId) {
        periodRepository.findOpenPeriods(ownerEmail)
                .stream()
                .filter(period -> keepOpenPeriodId == null || !period.getId().equals(keepOpenPeriodId))
                .forEach(period -> {
                    period.setStatus(FinancialPeriodStatus.SCHEDULED);
                    period.setClosedAt(null);
                });
    }

    private void validateCanClose(FinancialPeriod period) {
        List<MonthlyPlanItem> items = planItemRepository.findByOwnerEmailAndPeriod(period.getOwner().getEmail(), period.getId(), null);
        long pending = items.stream()
                .filter(this::includedInMainTotals)
                .filter(item -> item.getStatus() == MonthlyPlanItemStatus.PENDING || item.getStatus() == MonthlyPlanItemStatus.PARTIALLY_PAID)
                .count();
        if (pending > 0) {
            throw new BusinessException("Não é possível fechar um ciclo com contas ou rendas pendentes. Dê baixa, cancele ou ajuste esses itens antes de fechar.");
        }
    }

    private void validatePeriodRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Informe data inicial e final do ciclo financeiro.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("A data final do ciclo não pode ser anterior à data inicial.");
        }
    }

    private void validateNoOverlap(String ownerEmail, Long periodId, LocalDate startDate, LocalDate endDate) {
        List<FinancialPeriod> overlaps = periodRepository.findOverlappingPeriods(ownerEmail, periodId, startDate, endDate);
        if (!overlaps.isEmpty()) {
            FinancialPeriod overlap = overlaps.get(0);
            throw new BusinessException("O intervalo informado conflita com o ciclo \"" + overlap.getName() + "\".");
        }
    }

    private LocalDate clampDate(LocalDate date, FinancialPeriod period) {
        if (date.isBefore(period.getStartDate())) {
            return period.getStartDate();
        }
        if (date.isAfter(period.getEndDate())) {
            return period.getEndDate();
        }
        return date;
    }

    private LocalDate validateRecurrenceEndDate(boolean recurring, LocalDate recurrenceEndDate, LocalDate dueDate) {
        if (!recurring) {
            return null;
        }
        if (recurrenceEndDate != null && dueDate != null && recurrenceEndDate.isBefore(dueDate)) {
            throw new BusinessException("A data limite da recorrência não pode ser anterior ao vencimento inicial.");
        }
        return recurrenceEndDate;
    }

    private TransactionType parseTransactionType(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Informe o tipo: INCOME ou EXPENSE.");
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "INCOME", "RECEITA", "ENTRADA", "GANHO", "SALARIO", "SALÁRIO" -> TransactionType.INCOME;
            case "EXPENSE", "DESPESA", "SAIDA", "SAÍDA", "GASTO", "PAGAMENTO" -> TransactionType.EXPENSE;
            default -> TransactionType.valueOf(normalized);
        };
    }

    private MonthlyPlanItemNature parseNatureOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return MonthlyPlanItemNature.VARIABLE;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "FIXED", "FIXA", "FIXO" -> MonthlyPlanItemNature.FIXED;
            case "VARIABLE", "VARIAVEL", "VARIÁVEL", "VARIAVEIS", "VARIÁVEIS" -> MonthlyPlanItemNature.VARIABLE;
            case "CREDIT_CARD", "CARTAO", "CARTÃO", "CARTAO_CREDITO", "CARTÃO_CRÉDITO", "FATURA" -> MonthlyPlanItemNature.CREDIT_CARD;
            case "INVESTMENT", "INVESTIMENTO", "INVESTIMENTOS", "PRODUTO_FINANCEIRO", "CAPITALIZACAO", "CAPITALIZAÇÃO" -> MonthlyPlanItemNature.INVESTMENT;
            default -> MonthlyPlanItemNature.valueOf(normalized);
        };
    }

    private LocalDate parseDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value.trim());
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String appendNote(String current, String addition) {
        String normalized = normalizeNullable(addition);
        if (normalized == null) {
            return normalizeNullable(current);
        }
        String base = normalizeNullable(current);
        String value = base == null ? normalized : base + "\n" + normalized;
        return value.length() > 2000 ? value.substring(0, 2000) : value;
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
        return normalized.replaceAll("\\s+", " " );
    }

    private void recalculatePlanItemStatus(MonthlyPlanItem item) {
        if (item.getStatus() == MonthlyPlanItemStatus.CANCELED) {
            return;
        }
        BigDecimal actual = nullToZero(item.getActualAmount());
        BigDecimal expected = nullToZero(item.getExpectedAmount());
        if (actual.signum() <= 0) {
            item.setStatus(MonthlyPlanItemStatus.PENDING);
            item.setPaidOn(null);
            return;
        }
        if (actual.compareTo(expected) >= 0) {
            item.setStatus(MonthlyPlanItemStatus.PAID);
            if (item.getPaidOn() == null) {
                item.setPaidOn(LocalDate.now());
            }
            return;
        }
        item.setStatus(MonthlyPlanItemStatus.PARTIALLY_PAID);
    }

    private void rejectDirectSettlementStatus(MonthlyPlanItemStatus requestedStatus, MonthlyPlanItemStatus currentStatus) {
        if ((requestedStatus == MonthlyPlanItemStatus.PAID || requestedStatus == MonthlyPlanItemStatus.PARTIALLY_PAID)
                && requestedStatus != currentStatus) {
            throw new BusinessException("Use a baixa, recebimento ou conciliacao para marcar um item como pago ou parcialmente pago.");
        }
    }

    private void ensurePlanningEditable(FinancialPeriod period) {
        if (period.getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Não é possível alterar planejamento de um ciclo financeiro fechado. Reabra o ciclo primeiro.");
        }
    }

    private List<MonthlyPlanItemResponse> toTreeResponses(List<MonthlyPlanItem> items, MonthlyPlanItemStatus statusFilter) {
        Map<Long, List<MonthlyPlanItem>> childrenByParent = new HashMap<>();
        List<MonthlyPlanItem> roots = new ArrayList<>();
        for (MonthlyPlanItem item : items) {
            MonthlyPlanItem parent = item.getParentItem();
            if (parent == null) {
                roots.add(item);
            } else {
                childrenByParent.computeIfAbsent(parent.getId(), ignored -> new ArrayList<>()).add(item);
            }
        }
        Comparator<MonthlyPlanItem> order = Comparator
                .comparing(MonthlyPlanItem::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MonthlyPlanItem::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        roots.sort(order);
        childrenByParent.values().forEach(children -> children.sort(order));

        List<MonthlyPlanItemResponse> responses = new ArrayList<>();
        for (MonthlyPlanItem root : roots) {
            List<MonthlyPlanItemResponse> children = childrenByParent.getOrDefault(root.getId(), List.of())
                    .stream()
                    .filter(child -> statusFilter == null || child.getStatus() == statusFilter)
                    .map(MonthlyPlanItemResponse::from)
                    .toList();
            boolean rootMatches = statusFilter == null || root.getStatus() == statusFilter;
            if (rootMatches || !children.isEmpty()) {
                responses.add(MonthlyPlanItemResponse.from(root, children));
            }
        }
        return responses;
    }

    private MonthlyPlanItemResponse toSingleTreeResponse(String ownerEmail, Long rootItemId) {
        MonthlyPlanItem root = findOwnedPlanItem(ownerEmail, rootItemId);
        List<MonthlyPlanItemResponse> children = planItemRepository.findChildrenByParentIdAndOwnerEmail(root.getId(), ownerEmail)
                .stream()
                .map(MonthlyPlanItemResponse::from)
                .toList();
        return MonthlyPlanItemResponse.from(root, children);
    }

    private boolean includedInMainTotals(MonthlyPlanItem item) {
        return item.getAggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD;
    }

    private MonthlyPlanItemNature resolveNature(MonthlyPlanItemNature requested, MonthlyPlanItemAggregationType aggregationType) {
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_PARENT && requested == null) {
            return MonthlyPlanItemNature.CREDIT_CARD;
        }
        return requested == null ? MonthlyPlanItemNature.VARIABLE : requested;
    }

    private MonthlyPlanItemAggregationType resolveAggregationType(MonthlyPlanItemAggregationType requested, Long parentItemId) {
        if (parentItemId != null) {
            return MonthlyPlanItemAggregationType.GROUP_CHILD;
        }
        return requested == null ? MonthlyPlanItemAggregationType.NORMAL : requested;
    }

    private MonthlyPlanItemAggregationType resolveTargetAggregationType(MonthlyPlanItem item, MonthlyPlanItemAggregationType requested, Long parentItemId) {
        if (requested == null && parentItemId == null) {
            return item.getAggregationType() == null ? MonthlyPlanItemAggregationType.NORMAL : item.getAggregationType();
        }
        return resolveAggregationType(requested, parentItemId);
    }

    private MonthlyPlanItemInvoiceContributionMode resolveInvoiceContributionMode(
            MonthlyPlanItemInvoiceContributionMode requested,
            MonthlyPlanItemAggregationType aggregationType
    ) {
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            return requested == null ? MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY : requested;
        }
        return MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY;
    }

    private void applyRequestedHierarchy(
            String ownerEmail,
            MonthlyPlanItem item,
            MonthlyPlanItemAggregationType requestedAggregationType,
            Long requestedParentItemId,
            MonthlyPlanItemInvoiceContributionMode requestedContributionMode
    ) {
        if (requestedAggregationType == null && requestedParentItemId == null) {
            return;
        }
        MonthlyPlanItemAggregationType targetAggregationType = resolveAggregationType(requestedAggregationType, requestedParentItemId);
        if (targetAggregationType == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            MonthlyPlanItem parent = requestedParentItemId == null ? item.getParentItem() : findOwnedPlanItem(ownerEmail, requestedParentItemId);
            if (parent == null) {
                throw new BusinessException("Selecione a fatura vinculada para transformar este item em item interno.");
            }
            linkChildToParent(parent, item, requestedContributionMode);
            return;
        }
        if (requestedParentItemId != null) {
            throw new BusinessException("Itens normais ou faturas manuais não devem ter fatura vinculada.");
        }
        applyAggregationType(item, targetAggregationType);
    }

    private void applyAggregationType(MonthlyPlanItem item, MonthlyPlanItemAggregationType aggregationType) {
        if (aggregationType == null) {
            return;
        }
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_CHILD) {
            if (item.getParentItem() == null) {
                throw new BusinessException("Para transformar um item em item interno, vincule-o a uma fatura.");
            }
            item.setAggregationType(MonthlyPlanItemAggregationType.GROUP_CHILD);
            item.setRecurring(false);
            item.setInvoiceBaseAmount(null);
            return;
        }
        if (item.getParentItem() != null) {
            if (aggregationType == MonthlyPlanItemAggregationType.NORMAL) {
                item.setParentItem(null);
                item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
                item.setInvoiceContributionMode(MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY);
                item.setInvoiceBaseAmount(null);
                if (item.isPaidByParent()) {
                    item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    item.setPaidOn(null);
                    item.setStatus(MonthlyPlanItemStatus.PENDING);
                }
                item.setPaidByParent(false);
                item.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
                return;
            }
            throw new BusinessException("Desvincule o item da fatura antes de alterar o agrupamento.");
        }
        if (aggregationType == MonthlyPlanItemAggregationType.NORMAL && item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            List<MonthlyPlanItem> children = planItemRepository.findChildrenByParentIdAndOwnerEmail(item.getId(), item.getOwner().getEmail());
            if (!children.isEmpty()) {
                throw new BusinessException("Desvincule os itens internos antes de transformar a fatura em item normal.");
            }
        }
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_PARENT && item.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("A fatura manual precisa ser um item de despesa.");
        }
        item.setAggregationType(aggregationType);
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            if (item.getNature() == MonthlyPlanItemNature.VARIABLE) {
                item.setNature(MonthlyPlanItemNature.CREDIT_CARD);
            }
            if (item.getInvoiceBaseAmount() == null) {
                item.setInvoiceBaseAmount(nullToZero(item.getExpectedAmount()));
            }
            item.setInvoiceContributionMode(MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY);
        } else {
            item.setInvoiceBaseAmount(null);
            item.setInvoiceContributionMode(MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY);
        }
    }

    private void applyInvoiceFieldsAfterHierarchy(MonthlyPlanItem item, MonthlyPlanItemInvoiceContributionMode requestedContributionMode) {
        MonthlyPlanItemAggregationType aggregationType = item.getAggregationType() == null
                ? MonthlyPlanItemAggregationType.NORMAL
                : item.getAggregationType();
        if (aggregationType == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            if (item.getInvoiceBaseAmount() == null) {
                item.setInvoiceBaseAmount(nullToZero(item.getExpectedAmount()));
            }
            item.setInvoiceContributionMode(MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY);
            return;
        }
        item.setInvoiceBaseAmount(null);
        item.setInvoiceContributionMode(resolveInvoiceContributionMode(requestedContributionMode, aggregationType));
    }

    private void linkChildToParent(MonthlyPlanItem parent, MonthlyPlanItem child, MonthlyPlanItemInvoiceContributionMode contributionMode) {
        validateParentChildRelationship(parent, child);
        child.setParentItem(parent);
        child.setAggregationType(MonthlyPlanItemAggregationType.GROUP_CHILD);
        child.setInvoiceBaseAmount(null);
        child.setInvoiceContributionMode(resolveInvoiceContributionMode(contributionMode, MonthlyPlanItemAggregationType.GROUP_CHILD));
        child.setRecurring(false);
        child.setRecurrenceEndDate(null);
        child.setRecurringTemplateId(null);
        child.setGeneratedFromItemId(null);
        child.setRecurrenceKey(null);
        child.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        child.setPaidByParent(false);
    }

    private void synchronizeInvoiceTotal(MonthlyPlanItem parent) {
        if (parent == null || parent.getAggregationType() != MonthlyPlanItemAggregationType.GROUP_PARENT) {
            return;
        }
        BigDecimal baseAmount = nullToZero(parent.getInvoiceBaseAmount() == null ? parent.getExpectedAmount() : parent.getInvoiceBaseAmount());
        parent.setInvoiceBaseAmount(baseAmount);
        BigDecimal addedByChildren = planItemRepository.findChildrenByParentIdAndOwnerEmail(parent.getId(), parent.getOwner().getEmail())
                .stream()
                .filter(child -> child.getStatus() != MonthlyPlanItemStatus.CANCELED)
                .filter(child -> child.getInvoiceContributionMode() == MonthlyPlanItemInvoiceContributionMode.ADDS_TO_INVOICE_TOTAL)
                .map(MonthlyPlanItem::getExpectedAmount)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        parent.setExpectedAmount(baseAmount.add(addedByChildren).setScale(2, RoundingMode.HALF_UP));
        recalculatePlanItemStatus(parent);
    }

    private void validateInvoiceParent(MonthlyPlanItem invoice) {
        if (invoice.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("A fatura precisa ser um item de despesa.");
        }
        if (invoice.getAggregationType() != MonthlyPlanItemAggregationType.GROUP_PARENT) {
            throw new BusinessException("O item pai precisa estar marcado como fatura manual / item agrupador.");
        }
        if (invoice.getParentItem() != null) {
            throw new BusinessException("Um item interno não pode ser usado como fatura pai.");
        }
        if (invoice.getStatus() == MonthlyPlanItemStatus.CANCELED) {
            throw new BusinessException("Não é possível usar uma fatura cancelada.");
        }
    }

    private void validateParentChildRelationship(MonthlyPlanItem parent, MonthlyPlanItem child) {
        validateInvoiceParent(parent);
        if (Objects.equals(parent.getId(), child.getId())) {
            throw new BusinessException("A fatura não pode ser vinculada a ela mesma.");
        }
        if (!Objects.equals(parent.getOwner().getId(), child.getOwner().getId())) {
            throw new BusinessException("A fatura e o item interno precisam pertencer ao mesmo usuário.");
        }
        if (!Objects.equals(parent.getFinancialPeriod().getId(), child.getFinancialPeriod().getId())) {
            throw new BusinessException("A fatura e o item interno precisam estar no mesmo ciclo mensal.");
        }
        if (child.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("Somente despesas podem ser vinculadas a uma fatura de cartão.");
        }
        if (child.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT) {
            throw new BusinessException("Uma fatura não pode ser vinculada como item interno de outra fatura.");
        }
        if (child.getStatus() == MonthlyPlanItemStatus.CANCELED) {
            throw new BusinessException("Não é possível vincular um item cancelado.");
        }
    }

    private void synchronizeChildrenWithParentPayment(MonthlyPlanItem parent) {
        List<MonthlyPlanItem> children = planItemRepository.findChildrenByParentIdAndOwnerEmail(parent.getId(), parent.getOwner().getEmail());
        if (children.isEmpty()) {
            return;
        }
        if (parent.getStatus() == MonthlyPlanItemStatus.PAID) {
            for (MonthlyPlanItem child : children) {
                if (child.getStatus() == MonthlyPlanItemStatus.CANCELED) {
                    continue;
                }
                child.setActualAmount(nullToZero(child.getExpectedAmount()));
                child.setPaidOn(parent.getPaidOn() == null ? LocalDate.now() : parent.getPaidOn());
                child.setStatus(MonthlyPlanItemStatus.PAID);
                child.setPaidByParent(true);
                child.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.PARENT);
            }
            synchronizeInstallmentEntriesWithInvoiceChildren(parent, children);
            return;
        }
        if (parent.getStatus() == MonthlyPlanItemStatus.PENDING || parent.getStatus() == MonthlyPlanItemStatus.PARTIALLY_PAID) {
            for (MonthlyPlanItem child : children) {
                if (!child.isPaidByParent()) {
                    continue;
                }
                child.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                child.setPaidOn(null);
                child.setStatus(MonthlyPlanItemStatus.PENDING);
                child.setPaidByParent(false);
                child.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
            }
            synchronizeInstallmentEntriesWithInvoiceChildren(parent, children);
        }
    }

    private void synchronizeInstallmentEntriesWithInvoiceChildren(MonthlyPlanItem parent, List<MonthlyPlanItem> children) {
        List<Long> childIds = children.stream()
                .map(MonthlyPlanItem::getId)
                .filter(Objects::nonNull)
                .toList();
        if (childIds.isEmpty()) {
            return;
        }
        List<InstallmentPurchaseEntry> entries = installmentEntryRepository.findByMonthlyPlanItemIdsAndOwnerEmail(childIds, parent.getOwner().getEmail());
        if (parent.getStatus() == MonthlyPlanItemStatus.PAID) {
            LocalDate paidOn = parent.getPaidOn() == null ? LocalDate.now() : parent.getPaidOn();
            for (InstallmentPurchaseEntry entry : entries) {
                if (entry.getStatus() == InstallmentEntryStatus.CANCELED) {
                    continue;
                }
                entry.setStatus(InstallmentEntryStatus.PAID);
                entry.setPaidOn(paidOn);
                entry.setPaymentSource(InstallmentPaymentSource.PARENT_INVOICE);
                entry.setPaidBy(null);
                entry.setPaymentRegisteredAt(Instant.now());
            }
            return;
        }
        for (InstallmentPurchaseEntry entry : entries) {
            if (entry.getPaymentSource() != InstallmentPaymentSource.PARENT_INVOICE) {
                continue;
            }
            entry.setStatus(InstallmentEntryStatus.POSTED);
            entry.setPaidOn(null);
            entry.setPaymentSource(InstallmentPaymentSource.NONE);
            entry.setPaidBy(null);
            entry.setPaymentRegisteredAt(null);
        }
    }

    private void validateDueDate(FinancialPeriod period, LocalDate dueDate) {
        if (dueDate == null) {
            throw new BusinessException("A data de vencimento/recebimento é obrigatória.");
        }
        if (dueDate.isBefore(period.getStartDate()) || dueDate.isAfter(period.getEndDate())) {
            throw new BusinessException("A data deve estar dentro do período financeiro selecionado.");
        }
    }

    private void validateCategoryType(Category category, TransactionType type) {
        if (category != null && category.getType() != type) {
            throw new BusinessException("A categoria informada não pertence ao tipo selecionado.");
        }
    }

    private String defaultPeriodName(LocalDate date) {
        return "Ciclo iniciado em " + date.format(DEFAULT_NAME_FORMATTER);
    }

    private Integer normalizeTurnoverDay(Integer day) {
        if (day == null) {
            return null;
        }
        if (day < 1 || day > 31) {
            throw new BusinessException("O dia de virada precisa estar entre 1 e 31.");
        }
        return day;
    }

    private BigDecimal normalizeZeroOrPositive(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessException("O valor deve ser maior ou igual a zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
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

    private String normalizeNullableOrDefault(String value, String defaultValue) {
        String normalized = normalizeNullable(value);
        return normalized == null ? defaultValue : normalized;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
