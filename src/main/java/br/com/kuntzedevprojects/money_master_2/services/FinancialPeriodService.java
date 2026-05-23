package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodTurnoverRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialPeriodRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;

@Service
public class FinancialPeriodService {

    private static final DateTimeFormatter DEFAULT_NAME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinancialPeriodRepository periodRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public FinancialPeriodService(
            FinancialPeriodRepository periodRepository,
            MonthlyPlanItemRepository planItemRepository,
            FinancialTransactionRepository transactionRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService
    ) {
        this.periodRepository = periodRepository;
        this.planItemRepository = planItemRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
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
        return FinancialPeriodResponse.from(period);
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

        BigDecimal plannedIncome = sumExpected(items, TransactionType.INCOME);
        BigDecimal plannedExpense = sumExpected(items, TransactionType.EXPENSE);
        BigDecimal paidIncome = sumActual(items, TransactionType.INCOME);
        BigDecimal paidExpense = sumActual(items, TransactionType.EXPENSE);
        BigDecimal realizedIncomeSafe = sumTransactions(transactions, TransactionType.INCOME);
        BigDecimal realizedExpenseSafe = sumTransactions(transactions, TransactionType.EXPENSE);
        BigDecimal pendingIncome = plannedIncome.subtract(paidIncome).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingExpense = plannedExpense.subtract(paidExpense).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal plannedAvailable = plannedIncome.subtract(plannedExpense).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unplannedIncome = sumUnplannedProjectedImpact(transactions, items, TransactionType.INCOME);
        BigDecimal unplannedExpense = sumUnplannedProjectedImpact(transactions, items, TransactionType.EXPENSE);
        BigDecimal projectedAvailable = plannedAvailable
                .add(unplannedIncome)
                .subtract(unplannedExpense)
                .setScale(2, RoundingMode.HALF_UP);
        long pendingItems = items.stream().filter(item -> item.getStatus() == MonthlyPlanItemStatus.PENDING || item.getStatus() == MonthlyPlanItemStatus.PARTIALLY_PAID).count();
        long paidItems = items.stream().filter(item -> item.getStatus() == MonthlyPlanItemStatus.PAID).count();

        return new MonthlyPeriodSummaryResponse(
                FinancialPeriodResponse.from(period),
                plannedIncome,
                plannedExpense,
                paidIncome,
                paidExpense,
                pendingIncome,
                pendingExpense,
                realizedIncomeSafe,
                realizedExpenseSafe,
                plannedAvailable,
                unplannedIncome,
                unplannedExpense,
                projectedAvailable,
                pendingItems,
                paidItems
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyPlanItemResponse> listPlanItems(String ownerEmail, Long periodId, MonthlyPlanItemStatus status) {
        findOwnedPeriod(ownerEmail, periodId);
        return planItemRepository.findByOwnerEmailAndPeriod(ownerEmail, periodId, status)
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

        Account account = request.accountId() == null ? null : accountService.findOwnedAccount(ownerEmail, request.accountId());
        Category category = request.categoryId() == null ? null : categoryService.findAvailableCategory(ownerEmail, request.categoryId());
        validateCategoryType(category, request.type());

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
        item.setNature(request.nature() == null ? MonthlyPlanItemNature.VARIABLE : request.nature());
        item.setRecurring(Boolean.TRUE.equals(request.recurring()));
        item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), request.recurrenceEndDate(), item.getDueDate()));
        item.setStatus(request.status() == null ? MonthlyPlanItemStatus.PENDING : request.status());
        item.setNotes(normalizeNullable(request.notes()));
        if (item.getStatus() == MonthlyPlanItemStatus.PAID) {
            item.setActualAmount(item.getExpectedAmount());
            item.setPaidOn(item.getDueDate());
        }
        return MonthlyPlanItemResponse.from(planItemRepository.save(item));
    }

    @Transactional
    public MonthlyPlanItemResponse updatePlanItem(String ownerEmail, Long itemId, MonthlyPlanItemUpdateRequest request) {
        MonthlyPlanItem item = findOwnedPlanItem(ownerEmail, itemId);
        ensurePlanningEditable(item.getFinancialPeriod());

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
            item.setExpectedAmount(normalizeZeroOrPositive(request.expectedAmount()));
        }
        if (request.actualAmount() != null) {
            item.setActualAmount(normalizeZeroOrPositive(request.actualAmount()));
        }
        if (request.dueDate() != null) {
            validateDueDate(item.getFinancialPeriod(), request.dueDate());
            item.setDueDate(request.dueDate());
            item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), item.getRecurrenceEndDate(), item.getDueDate()));
        }
        if (request.paidOn() != null) {
            item.setPaidOn(request.paidOn());
        }
        if (request.nature() != null) {
            item.setNature(request.nature());
        }
        if (request.recurring() != null) {
            item.setRecurring(request.recurring());
            if (!item.isRecurring()) {
                item.setRecurrenceEndDate(null);
            }
        }
        if (request.recurrenceEndDate() != null || request.recurring() != null) {
            item.setRecurrenceEndDate(validateRecurrenceEndDate(item.isRecurring(), request.recurrenceEndDate(), item.getDueDate()));
        }
        if (request.status() != null) {
            item.setStatus(request.status());
            if (request.status() == MonthlyPlanItemStatus.PAID && item.getActualAmount().signum() == 0) {
                item.setActualAmount(item.getExpectedAmount());
            }
            if (request.status() == MonthlyPlanItemStatus.PAID && item.getPaidOn() == null) {
                item.setPaidOn(LocalDate.now());
            }
        }
        if (request.notes() != null) {
            item.setNotes(normalizeNullable(request.notes()));
        }
        recalculatePlanItemStatus(item);
        return MonthlyPlanItemResponse.from(item);
    }

    @Transactional
    public void cancelPlanItem(String ownerEmail, Long itemId) {
        MonthlyPlanItem item = findOwnedPlanItem(ownerEmail, itemId);
        ensurePlanningEditable(item.getFinancialPeriod());
        item.setStatus(MonthlyPlanItemStatus.CANCELED);
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
        Account account = accountName == null || accountName.isBlank() ? null : accountService.resolveForAi(ownerEmail, null, accountName);
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
        BigDecimal actual = transactionRepository.sumAmountByMonthlyPlanItem(item.getId());
        item.setActualAmount(nullToZero(actual));
        item.setPaidOn(transactionRepository.findLatestPaymentDateByMonthlyPlanItem(item.getId()));
        recalculatePlanItemStatus(item);
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
        item.setAccount(account);
        item.setCategory(category);
        item.setType(type);
        item.setDescription(safeDescription);
        item.setExpectedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setDueDate(dueDate);
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(parseNatureOrDefault(natureText));
        item.setRecurring(recurring);
        item.setRecurrenceEndDate(validateRecurrenceEndDate(recurring, parseDateOrNull(recurrenceEndDateText), dueDate));
        item.setNotes(normalizeNullable(notes));
        return planItemRepository.save(item);
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
        if (targetPeriod.getId() != null && !planItemRepository.findByOwnerEmailAndPeriod(ownerEmail, targetPeriod.getId(), null).isEmpty()) {
            return;
        }
        List<MonthlyPlanItem> recurringItems = planItemRepository.findRecurringByOwnerEmailAndPeriod(ownerEmail, sourcePeriod.getId());
        recurringItems.forEach(source -> {
            LocalDate dueDate = copyDayIntoPeriod(source.getDueDate(), targetPeriod);
            if (source.getRecurrenceEndDate() != null && dueDate.isAfter(source.getRecurrenceEndDate())) {
                return;
            }
            MonthlyPlanItem item = new MonthlyPlanItem();
            item.setOwner(source.getOwner());
            item.setFinancialPeriod(targetPeriod);
            item.setAccount(source.getAccount());
            item.setCategory(source.getCategory());
            item.setType(source.getType());
            item.setDescription(source.getDescription());
            item.setExpectedAmount(source.getExpectedAmount());
            item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setDueDate(dueDate);
            item.setStatus(MonthlyPlanItemStatus.PENDING);
            item.setNature(source.getNature());
            item.setRecurring(true);
            item.setRecurrenceEndDate(source.getRecurrenceEndDate());
            item.setNotes(source.getNotes());
            planItemRepository.save(item);
        });
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

    private BigDecimal sumTransactions(List<FinancialTransaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o impacto real que ainda não está representado no planejamento.
     *
     * Itens planejados já entram em plannedIncome/plannedExpense. Por isso, uma
     * transação vinculada a um item mensal não deve entrar novamente aqui.
     * Compras no cartão de crédito também não entram quando há um item de fatura
     * no ciclo, porque o autoajuste já somou essas compras ao previsto da fatura.
     */
    private BigDecimal sumUnplannedProjectedImpact(List<FinancialTransaction> transactions, List<MonthlyPlanItem> items, TransactionType type) {
        boolean hasCreditCardPlanItem = items.stream().anyMatch(this::isCreditCardPlanItem);
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .filter(transaction -> transaction.getMonthlyPlanItem() == null)
                .filter(transaction -> !shouldIgnoreCreditCardPurchaseForProjection(transaction, hasCreditCardPlanItem))
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
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
        String description = item.getDescription();
        String categoryName = item.getCategory() == null ? "" : item.getCategory().getName();
        return item.getType() == TransactionType.EXPENSE
                && item.getNature() == MonthlyPlanItemNature.VARIABLE
                && item.getStatus() != MonthlyPlanItemStatus.CANCELED
                && (containsCreditCardText(description) || containsCreditCardText(categoryName));
    }

    private boolean containsCreditCardText(String value) {
        String normalized = normalizeComparable(value);
        return normalized.contains("cartao credito")
                || normalized.contains("cartao de credito")
                || normalized.contains("fatura cartao");
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

    private BigDecimal sumExpected(List<MonthlyPlanItem> items, TransactionType type) {
        return items.stream()
                .filter(item -> item.getType() == type)
                .filter(item -> item.getStatus() != MonthlyPlanItemStatus.CANCELED)
                .map(MonthlyPlanItem::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumActual(List<MonthlyPlanItem> items, TransactionType type) {
        return items.stream()
                .filter(item -> item.getType() == type)
                .filter(item -> item.getStatus() != MonthlyPlanItemStatus.CANCELED)
                .map(MonthlyPlanItem::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
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

    private void ensurePlanningEditable(FinancialPeriod period) {
        if (period.getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Não é possível alterar planejamento de um ciclo financeiro fechado. Reabra o ciclo primeiro.");
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
