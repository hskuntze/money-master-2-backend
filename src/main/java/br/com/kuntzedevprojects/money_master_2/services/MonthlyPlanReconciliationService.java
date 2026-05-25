package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemLinkTransactionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemPaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemReopenRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUnlinkTransactionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileCandidateResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;

@Service
public class MonthlyPlanReconciliationService {

    private static final int AUTO_LINK_SCORE = 75;
    private static final int POSSIBLE_LINK_SCORE = 55;

    private final MonthlyPlanItemRepository planItemRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final FinancialPeriodService financialPeriodService;
    private final FinancialTransactionService transactionService;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public MonthlyPlanReconciliationService(
            MonthlyPlanItemRepository planItemRepository,
            FinancialTransactionRepository transactionRepository,
            FinancialPeriodService financialPeriodService,
            FinancialTransactionService transactionService,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService
    ) {
        this.planItemRepository = planItemRepository;
        this.transactionRepository = transactionRepository;
        this.financialPeriodService = financialPeriodService;
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional
    public MonthlyPlanItemResponse registerPayment(String ownerEmail, Long itemId, MonthlyPlanItemPaymentRequest request) {
        MonthlyPlanItem item = financialPeriodService.findOwnedPlanItem(ownerEmail, itemId);
        ensureCanOperate(item);
        MonthlyPlanItemPaymentRequest safeRequest = request == null
                ? new MonthlyPlanItemPaymentRequest(null, null, null, null, null, true, null)
                : request;

        if (safeRequest.transactionId() != null) {
            return linkTransaction(ownerEmail, itemId, new MonthlyPlanItemLinkTransactionRequest(
                    safeRequest.transactionId(), true, false, false
            ));
        }

        BigDecimal amount = normalizeAmount(safeRequest.amount() == null
                ? remainingOrExpected(item)
                : safeRequest.amount());
        LocalDate occurredOn = safeRequest.occurredOn() == null ? LocalDate.now() : safeRequest.occurredOn();

        if (!Boolean.FALSE.equals(safeRequest.preferExistingTransaction())) {
            Optional<FinancialTransaction> existing = findBestTransactionForItem(ownerEmail, item, amount, occurredOn, true);
            if (existing.isPresent()) {
                linkTransactionEntity(ownerEmail, item, existing.get(), true, false, false);
                return MonthlyPlanItemResponse.from(item);
            }
        }

        if (item.getFinancialPeriod().getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Este ciclo financeiro está fechado. Para dar baixa, associe uma transação já existente em vez de criar um novo lançamento.");
        }

        Long accountId = safeRequest.accountId() != null
                ? safeRequest.accountId()
                : item.getAccount() == null ? null : item.getAccount().getId();
        Long categoryId = safeRequest.categoryId() != null
                ? safeRequest.categoryId()
                : item.getCategory() == null ? null : item.getCategory().getId();

        FinancialTransactionCreateRequest createRequest = new FinancialTransactionCreateRequest(
                accountId,
                categoryId,
                item.getFinancialPeriod().getId(),
                item.getId(),
                item.getType(),
                item.getDescription(),
                amount,
                occurredOn,
                TransactionSource.MANUAL,
                appendNote(safeRequest.notes(), "Baixa criada a partir do planejamento mensal.")
        );
        transactionService.create(ownerEmail, createRequest);
        return MonthlyPlanItemResponse.from(financialPeriodService.findOwnedPlanItem(ownerEmail, itemId));
    }

    @Transactional
    public MonthlyPlanItemResponse linkTransaction(String ownerEmail, Long itemId, MonthlyPlanItemLinkTransactionRequest request) {
        if (request == null || request.transactionId() == null) {
            throw new BusinessException("Informe a transação que deve ser associada ao item planejado.");
        }
        MonthlyPlanItem item = financialPeriodService.findOwnedPlanItem(ownerEmail, itemId);
        ensurePeriodEditable(item);
        FinancialTransaction transaction = transactionService.findOwnedTransaction(ownerEmail, request.transactionId());
        linkTransactionEntity(ownerEmail, item, transaction, !Boolean.FALSE.equals(request.copyCategoryFromPlanItem()), Boolean.TRUE.equals(request.copyAccountFromPlanItem()), Boolean.TRUE.equals(request.forceRelink()));
        return MonthlyPlanItemResponse.from(item);
    }


    @Transactional
    public MonthlyPlanItemResponse reopenPlanItem(String ownerEmail, Long itemId, MonthlyPlanItemReopenRequest request) {
        MonthlyPlanItem item = financialPeriodService.findOwnedPlanItem(ownerEmail, itemId);
        ensurePeriodEditable(item);
        MonthlyPlanItemReopenRequest safeRequest = request == null
                ? new MonthlyPlanItemReopenRequest(false, true, null)
                : request;

        List<FinancialTransaction> linkedTransactions = transactionRepository.findByMonthlyPlanItemIdWithDetails(item.getId());
        if (safeRequest.shouldDeleteLinkedTransactions()) {
            for (FinancialTransaction transaction : linkedTransactions) {
                validateTransactionOwnership(ownerEmail, transaction);
                transactionRepository.delete(transaction);
            }
            transactionRepository.flush();
        } else if (safeRequest.shouldKeepTransactionsUnlinked()) {
            for (FinancialTransaction transaction : linkedTransactions) {
                validateTransactionOwnership(ownerEmail, transaction);
                transaction.setMonthlyPlanItem(null);
            }
        }

        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setPaidOn(null);
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNotes(appendNote(item.getNotes(), safeRequest.notes()));
        return MonthlyPlanItemResponse.from(item);
    }

    @Transactional
    public MonthlyPlanItemResponse unlinkTransaction(String ownerEmail, Long itemId, MonthlyPlanItemUnlinkTransactionRequest request) {
        if (request == null || request.transactionId() == null) {
            throw new BusinessException("Informe a transação que deve ser desvinculada.");
        }
        MonthlyPlanItem item = financialPeriodService.findOwnedPlanItem(ownerEmail, itemId);
        ensurePeriodEditable(item);
        FinancialTransaction transaction = transactionService.findOwnedTransaction(ownerEmail, request.transactionId());
        if (transaction.getMonthlyPlanItem() == null || !transaction.getMonthlyPlanItem().getId().equals(item.getId())) {
            throw new BusinessException("Esta transação não está associada ao item planejado informado.");
        }
        if (request.shouldDeleteTransaction()) {
            transactionRepository.delete(transaction);
            transactionRepository.flush();
        } else {
            transaction.setMonthlyPlanItem(null);
            transaction.setNotes(appendNote(transaction.getNotes(), request.notes()));
        }
        financialPeriodService.synchronizePlanItemPayment(item);
        return MonthlyPlanItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<FinancialTransactionResponse> listUnlinkedTransactions(String ownerEmail, Long periodId, TransactionType type) {
        return listTransactionsForAssociation(ownerEmail, periodId, type, true);
    }

    @Transactional(readOnly = true)
    public List<FinancialTransactionResponse> listTransactionsForAssociation(String ownerEmail, Long periodId, TransactionType type, boolean onlyUnlinked) {
        FinancialPeriod period = financialPeriodService.findOwnedPeriod(ownerEmail, periodId);
        return transactionRepository.findForMonthlyPlanReconciliation(
                        ownerEmail,
                        period.getId(),
                        period.getStartDate(),
                        period.getEndDate(),
                        type,
                        onlyUnlinked
                )
                .stream()
                .map(FinancialTransactionResponse::from)
                .toList();
    }

    @Transactional
    public MonthlyPlanReconcileResponse reconcile(String ownerEmail, Long periodId, MonthlyPlanReconcileRequest request) {
        MonthlyPlanReconcileRequest safeRequest = request == null
                ? new MonthlyPlanReconcileRequest(true, false, true, true, MonthlyPlanItemNature.VARIABLE, false, null, null, null, false, false)
                : request;
        FinancialPeriod period = financialPeriodService.findOwnedPeriod(ownerEmail, periodId);
        if (period.getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Este ciclo financeiro está fechado. Reabra o ciclo antes de conciliar transações.");
        }
        boolean dryRun = safeRequest.isDryRun();
        LocalDate from = safeRequest.from() == null ? period.getStartDate() : safeRequest.from();
        LocalDate to = safeRequest.to() == null ? period.getEndDate() : safeRequest.to();
        if (from.isAfter(to)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }

        List<FinancialTransaction> transactions = transactionRepository.findForMonthlyPlanReconciliation(
                ownerEmail,
                period.getId(),
                from,
                to,
                safeRequest.type(),
                safeRequest.shouldOnlyUnlinkedTransactions()
        );
        List<MonthlyPlanItem> planItems = planItemRepository.findActiveCandidatesByOwnerEmailAndPeriod(ownerEmail, period.getId(), safeRequest.type());
        List<MonthlyPlanReconcileCandidateResponse> candidates = new ArrayList<>();
        Set<Long> linkedOrReservedItems = new HashSet<>();

        int matched = 0;
        int linked = 0;
        int created = 0;
        int ambiguous = 0;
        int ignored = 0;

        for (FinancialTransaction transaction : transactions) {
            if (transaction.getMonthlyPlanItem() != null && safeRequest.shouldOnlyUnlinkedTransactions()) {
                ignored++;
                continue;
            }

            MatchResult match = bestPlanItemForTransaction(transaction, planItems, linkedOrReservedItems);
            if (match.item() != null && match.score() >= AUTO_LINK_SCORE && safeRequest.shouldLinkExistingTransactions()) {
                matched++;
                linkedOrReservedItems.add(match.item().getId());
                boolean executed = false;
                if (!dryRun) {
                    linkTransactionEntity(ownerEmail, match.item(), transaction, false, false, false);
                    executed = true;
                    linked++;
                }
                candidates.add(candidate(transaction, match.item(), match.score(), "LINK_EXISTING_TRANSACTION", executed,
                        executed ? "Transação associada ao item planejado." : "Transação pode ser associada automaticamente ao item planejado."));
                continue;
            }

            if (match.item() != null && match.score() >= POSSIBLE_LINK_SCORE) {
                ambiguous++;
                candidates.add(candidate(transaction, match.item(), match.score(), "NEEDS_CONFIRMATION", false,
                        "Possível correspondência. Confirme antes de associar."));
                continue;
            }

            if (safeRequest.shouldCreateMissingPlanItems()) {
                boolean executed = false;
                MonthlyPlanItem createdItem = null;
                boolean planningOnly = safeRequest.shouldCreatePlanItemsAsPendingOnly();
                if (!dryRun) {
                    createdItem = createPlanItemFromTransaction(ownerEmail, period, transaction, safeRequest);
                    planItems.add(createdItem);
                    linkedOrReservedItems.add(createdItem.getId());
                    created++;
                    if (planningOnly) {
                        if (safeRequest.shouldDeleteSourceTransactionsWhenCreatingPlanItems()) {
                            transactionRepository.delete(transaction);
                        }
                    } else {
                        linkTransactionEntity(ownerEmail, createdItem, transaction, false, false, false);
                        linked++;
                    }
                    executed = true;
                }
                candidates.add(new MonthlyPlanReconcileCandidateResponse(
                        transaction.getId(),
                        transaction.getDescription(),
                        transaction.getAmount(),
                        transaction.getOccurredOn(),
                        createdItem == null ? null : createdItem.getId(),
                        createdItem == null ? transaction.getDescription() : createdItem.getDescription(),
                        transaction.getAmount(),
                        transaction.getOccurredOn(),
                        0,
                        planningOnly ? "CREATE_PLAN_ITEM_AS_PENDING" : "CREATE_PLAN_ITEM_AND_LINK",
                        executed,
                        executed
                                ? (planningOnly
                                        ? "Item planejado pendente criado com base na transação. A transação original foi " + (safeRequest.shouldDeleteSourceTransactionsWhenCreatingPlanItems() ? "excluída." : "mantida avulsa.")
                                        : "Item planejado criado e transação associada.")
                                : (planningOnly
                                        ? "Nenhum item planejado compatível encontrado. Posso criar um item pendente com base nesta transação, sem dar baixa."
                                        : "Nenhum item planejado compatível encontrado. Posso criar um item com base nesta transação e associar a baixa.")
                ));
                continue;
            }

            ignored++;
            candidates.add(candidate(transaction, null, 0, "IGNORED", false,
                    "Nenhum item planejado compatível encontrado."));
        }

        String message = dryRun
                ? "Prévia de conciliação gerada. Execute apenas se as associações estiverem corretas."
                : "Conciliação executada. Transações associadas e itens atualizados.";
        return new MonthlyPlanReconcileResponse(
                dryRun,
                period.getId(),
                period.getName(),
                transactions.size(),
                matched,
                created,
                linked,
                ambiguous,
                ignored,
                candidates,
                message,
                Instant.now()
        );
    }

    @Transactional
    public MonthlyPlanItemResponse createPlanItemFromAi(
            String ownerEmail,
            Long periodId,
            String typeText,
            String description,
            BigDecimal expectedAmount,
            String dueDateText,
            String accountName,
            String categoryName,
            String natureText,
            Boolean recurring,
            String notes
    ) {
        TransactionType type = parseTransactionType(typeText);
        LocalDate dueDate = parseDateOrToday(dueDateText);
        FinancialPeriod period = periodId == null
                ? financialPeriodService.findOrCreateForDate(ownerEmail, dueDate)
                : financialPeriodService.findOwnedPeriod(ownerEmail, periodId);
        if (dueDate.isBefore(period.getStartDate()) || dueDate.isAfter(period.getEndDate())) {
            dueDate = clampDate(dueDate, period);
        }
        Account account = accountName == null || accountName.isBlank() ? null : accountService.resolveForAi(ownerEmail, null, accountName);
        Category category = categoryName == null || categoryName.isBlank() ? null : categoryService.resolveForAi(ownerEmail, null, categoryName, type);
        MonthlyPlanItemCreateRequest request = new MonthlyPlanItemCreateRequest(
                account == null ? null : account.getId(),
                category == null ? null : category.getId(),
                type,
                required(description, "Informe a descrição do item planejado."),
                expectedAmount == null ? BigDecimal.ZERO : expectedAmount,
                dueDate,
                parseNatureOrDefault(natureText),
                Boolean.TRUE.equals(recurring),
                null,
                MonthlyPlanItemStatus.PENDING,
                notes
        );
        return financialPeriodService.createPlanItem(ownerEmail, period.getId(), request);
    }

    @Transactional
    public MonthlyPlanItemResponse payPlanItemFromAi(
            String ownerEmail,
            Long planItemId,
            Long transactionId,
            Long periodId,
            String typeText,
            String description,
            BigDecimal amount,
            String occurredOnText,
            String accountName,
            String categoryName,
            String natureText,
            Boolean recurring,
            Boolean createIfMissing,
            Boolean preferExistingTransaction,
            String originalMessage,
            String notes
    ) {
        LocalDate occurredOn = parseDateOrToday(occurredOnText);
        MonthlyPlanItem item = planItemId == null
                ? resolvePlanItemForAi(ownerEmail, periodId, typeText, description, amount, occurredOn, accountName, categoryName, natureText, recurring, createIfMissing, notes)
                : financialPeriodService.findOwnedPlanItem(ownerEmail, planItemId);

        if (transactionId != null) {
            return linkTransaction(ownerEmail, item.getId(), new MonthlyPlanItemLinkTransactionRequest(transactionId, true, false, false));
        }

        Account account = accountName == null || accountName.isBlank()
                ? item.getAccount()
                : accountService.resolveForAi(ownerEmail, null, accountName);
        Category category = categoryName == null || categoryName.isBlank()
                ? item.getCategory()
                : categoryService.resolveForAi(ownerEmail, null, categoryName, item.getType());

        MonthlyPlanItemPaymentRequest request = new MonthlyPlanItemPaymentRequest(
                null,
                account == null ? null : account.getId(),
                category == null ? null : category.getId(),
                amount,
                occurredOn,
                !Boolean.FALSE.equals(preferExistingTransaction),
                appendNote(notes, originalMessage)
        );
        return registerPayment(ownerEmail, item.getId(), request);
    }


    @Transactional
    public MonthlyPlanItemResponse reopenPlanItemFromAi(
            String ownerEmail,
            Long planItemId,
            Long periodId,
            String typeText,
            String description,
            BigDecimal amount,
            String referenceDateText,
            String accountName,
            String categoryName,
            String natureText,
            Boolean deleteLinkedTransactions,
            Boolean createIfMissing,
            String notes
    ) {
        LocalDate referenceDate = parseDateOrToday(referenceDateText);
        MonthlyPlanItem item = planItemId == null
                ? resolvePlanItemForAi(ownerEmail, periodId, typeText, description, amount, referenceDate, accountName, categoryName, natureText, null, createIfMissing, notes)
                : financialPeriodService.findOwnedPlanItem(ownerEmail, planItemId);
        return reopenPlanItem(ownerEmail, item.getId(), new MonthlyPlanItemReopenRequest(deleteLinkedTransactions, true, notes));
    }

    @Transactional
    public MonthlyPlanItemResponse linkTransactionToPlanItemFromAi(
            String ownerEmail,
            Long planItemId,
            Long transactionId,
            Long periodId,
            String typeText,
            String planItemDescription,
            String transactionDescription,
            BigDecimal amount,
            String occurredOnText,
            String accountName,
            String categoryName,
            String natureText,
            Boolean recurring,
            Boolean createIfMissing,
            Boolean forceRelink,
            String notes
    ) {
        LocalDate occurredOn = parseDateOrToday(occurredOnText);
        FinancialTransaction transaction = transactionId == null
                ? resolveTransactionForAi(ownerEmail, periodId, typeText, transactionDescription == null ? planItemDescription : transactionDescription, amount, occurredOn)
                : transactionService.findOwnedTransaction(ownerEmail, transactionId);
        MonthlyPlanItem item = planItemId == null
                ? resolvePlanItemForAi(ownerEmail, periodId, transaction.getType().name(), planItemDescription == null ? transaction.getDescription() : planItemDescription,
                        amount == null ? transaction.getAmount() : amount, occurredOn, accountName, categoryName, natureText, recurring, createIfMissing, notes)
                : financialPeriodService.findOwnedPlanItem(ownerEmail, planItemId);
        linkTransactionEntity(ownerEmail, item, transaction, true, false, Boolean.TRUE.equals(forceRelink));
        return MonthlyPlanItemResponse.from(item);
    }

    private MonthlyPlanItem resolvePlanItemForAi(
            String ownerEmail,
            Long periodId,
            String typeText,
            String description,
            BigDecimal amount,
            LocalDate referenceDate,
            String accountName,
            String categoryName,
            String natureText,
            Boolean recurring,
            Boolean createIfMissing,
            String notes
    ) {
        TransactionType type = parseTransactionType(typeText);
        FinancialPeriod period = periodId == null
                ? financialPeriodService.findOrCreateForDate(ownerEmail, referenceDate)
                : financialPeriodService.findOwnedPeriod(ownerEmail, periodId);
        String safeDescription = required(description, "Informe a descrição da conta/renda planejada.");
        List<MonthlyPlanItem> directMatches = planItemRepository.findByDescriptionContainingInPeriod(ownerEmail, period.getId(), type, safeDescription);
        List<MonthlyPlanItem> candidates = directMatches.isEmpty()
                ? planItemRepository.findActiveCandidatesByOwnerEmailAndPeriod(ownerEmail, period.getId(), type)
                : directMatches;
        MatchResult best = candidates.stream()
                .map(item -> new MatchResult(item, score(safeDescription, amount, referenceDate, null, null, item)))
                .max(Comparator.comparingInt(MatchResult::score))
                .orElse(new MatchResult(null, 0));
        if (best.item() != null && best.score() >= POSSIBLE_LINK_SCORE) {
            return best.item();
        }
        if (!Boolean.TRUE.equals(createIfMissing)) {
            throw new ResourceNotFoundException("Não encontrei item planejado compatível. Cadastre o item ou peça para criar se não existir.");
        }
        MonthlyPlanItemResponse created = createPlanItemFromAi(
                ownerEmail,
                period.getId(),
                type.name(),
                safeDescription,
                amount == null ? BigDecimal.ZERO : amount,
                referenceDate.toString(),
                accountName,
                categoryName,
                natureText,
                recurring,
                notes
        );
        return financialPeriodService.findOwnedPlanItem(ownerEmail, created.id());
    }

    private FinancialTransaction resolveTransactionForAi(String ownerEmail, Long periodId, String typeText, String description, BigDecimal amount, LocalDate occurredOn) {
        TransactionType type = parseTransactionType(typeText);
        FinancialPeriod period = periodId == null
                ? financialPeriodService.findOrCreateForDate(ownerEmail, occurredOn)
                : financialPeriodService.findOwnedPeriod(ownerEmail, periodId);
        List<FinancialTransaction> transactions = transactionRepository.findForMonthlyPlanReconciliation(
                ownerEmail,
                period.getId(),
                period.getStartDate(),
                period.getEndDate(),
                type,
                true
        );
        Optional<FinancialTransaction> best = transactions.stream()
                .map(transaction -> new TransactionMatch(transaction, scoreTransaction(description, amount, occurredOn, transaction)))
                .filter(match -> match.score() >= POSSIBLE_LINK_SCORE)
                .max(Comparator.comparingInt(TransactionMatch::score))
                .map(TransactionMatch::transaction);
        return best.orElseThrow(() -> new ResourceNotFoundException("Não encontrei transação avulsa compatível para associar."));
    }

    private void linkTransactionEntity(String ownerEmail, MonthlyPlanItem item, FinancialTransaction transaction, boolean copyCategoryFromPlanItem, boolean copyAccountFromPlanItem, boolean forceRelink) {
        if (!Objects.equals(item.getOwner().getEmail().toLowerCase(Locale.ROOT), ownerEmail.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("O item planejado não pertence ao usuário autenticado.");
        }
        if (!Objects.equals(transaction.getOwner().getEmail().toLowerCase(Locale.ROOT), ownerEmail.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("A transação não pertence ao usuário autenticado.");
        }
        if (item.getType() != transaction.getType()) {
            throw new BusinessException("O tipo da transação precisa ser igual ao tipo do item planejado.");
        }
        MonthlyPlanItem previousPlanItem = transaction.getMonthlyPlanItem();
        if (previousPlanItem != null && !previousPlanItem.getId().equals(item.getId()) && !forceRelink) {
            throw new BusinessException("Esta transação já está associada a outro item planejado. Use forceRelink=true se quiser corrigir a associação.");
        }
        transaction.setMonthlyPlanItem(item);
        transaction.setFinancialPeriod(item.getFinancialPeriod());
        if (item.getAccount() == null) {
            item.setAccount(transaction.getAccount());
        } else if (copyAccountFromPlanItem) {
            transaction.setAccount(item.getAccount());
        }
        if (item.getCategory() == null && transaction.getCategory() != null) {
            item.setCategory(transaction.getCategory());
        } else if (copyCategoryFromPlanItem && item.getCategory() != null) {
            transaction.setCategory(item.getCategory());
        }
        financialPeriodService.synchronizePlanItemPayment(item);
        if (previousPlanItem != null && !previousPlanItem.getId().equals(item.getId())) {
            financialPeriodService.synchronizePlanItemPayment(previousPlanItem);
        }
    }

    private Optional<FinancialTransaction> findBestTransactionForItem(String ownerEmail, MonthlyPlanItem item, BigDecimal amount, LocalDate occurredOn, boolean requireHighConfidence) {
        List<FinancialTransaction> transactions = transactionRepository.findUnlinkedByOwnerPeriodAndType(
                ownerEmail,
                item.getFinancialPeriod().getId(),
                item.getType()
        );
        int minimumScore = requireHighConfidence ? AUTO_LINK_SCORE : POSSIBLE_LINK_SCORE;
        return transactions.stream()
                .map(transaction -> new TransactionMatch(transaction, scoreTransaction(item.getDescription(), amount, occurredOn, transaction)))
                .filter(match -> match.score() >= minimumScore)
                .max(Comparator.comparingInt(TransactionMatch::score))
                .map(TransactionMatch::transaction);
    }

    private MatchResult bestPlanItemForTransaction(FinancialTransaction transaction, List<MonthlyPlanItem> planItems, Set<Long> reservedItems) {
        return planItems.stream()
                .filter(item -> item.getType() == transaction.getType())
                .filter(item -> !reservedItems.contains(item.getId()) || item.getId().equals(transaction.getMonthlyPlanItem() == null ? null : transaction.getMonthlyPlanItem().getId()))
                .map(item -> new MatchResult(item, score(transaction.getDescription(), transaction.getAmount(), transaction.getOccurredOn(), transaction.getAccount(), transaction.getCategory(), item)))
                .max(Comparator.comparingInt(MatchResult::score))
                .orElse(new MatchResult(null, 0));
    }

    private MonthlyPlanItem createPlanItemFromTransaction(String ownerEmail, FinancialPeriod period, FinancialTransaction transaction, MonthlyPlanReconcileRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setOwner(owner);
        item.setFinancialPeriod(period);
        item.setAccount(transaction.getAccount());
        item.setCategory(transaction.getCategory());
        item.setType(transaction.getType());
        item.setDescription(transaction.getDescription());
        item.setExpectedAmount(transaction.getAmount().setScale(2, RoundingMode.HALF_UP));
        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setDueDate(clampDate(transaction.getOccurredOn(), period));
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(request.defaultNature() == null ? MonthlyPlanItemNature.VARIABLE : request.defaultNature());
        item.setRecurring(Boolean.TRUE.equals(request.recurring()));
        item.setNotes("Criado automaticamente pela conciliação mensal.");
        return planItemRepository.save(item);
    }

    private MonthlyPlanReconcileCandidateResponse candidate(FinancialTransaction transaction, MonthlyPlanItem item, int score, String action, boolean executed, String message) {
        return new MonthlyPlanReconcileCandidateResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getOccurredOn(),
                item == null ? null : item.getId(),
                item == null ? null : item.getDescription(),
                item == null ? null : item.getExpectedAmount(),
                item == null ? null : item.getDueDate(),
                score,
                action,
                executed,
                message
        );
    }

    private int score(String transactionDescription, BigDecimal amount, LocalDate occurredOn, Account account, Category category, MonthlyPlanItem item) {
        int score = 0;
        score += descriptionScore(transactionDescription, item.getDescription());
        score += amountScore(amount, item.getExpectedAmount());
        score += dateScore(occurredOn, item.getDueDate());
        if (category != null && item.getCategory() != null && category.getId().equals(item.getCategory().getId())) {
            score += 12;
        }
        if (account != null && item.getAccount() != null && account.getId().equals(item.getAccount().getId())) {
            score += 5;
        }
        return Math.min(100, score);
    }

    private int scoreTransaction(String expectedDescription, BigDecimal expectedAmount, LocalDate expectedDate, FinancialTransaction transaction) {
        int score = 0;
        score += descriptionScore(expectedDescription, transaction.getDescription());
        score += amountScore(expectedAmount, transaction.getAmount());
        score += dateScore(expectedDate, transaction.getOccurredOn());
        return Math.min(100, score);
    }

    private int descriptionScore(String left, String right) {
        String a = normalizeText(left);
        String b = normalizeText(right);
        if (a.isBlank() || b.isBlank()) {
            return 0;
        }
        if (a.equals(b)) {
            return 45;
        }
        if (a.contains(b) || b.contains(a)) {
            return 38;
        }
        Set<String> leftTokens = tokens(a);
        Set<String> rightTokens = tokens(b);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        long intersection = leftTokens.stream().filter(rightTokens::contains).count();
        double ratio = intersection / (double) Math.max(leftTokens.size(), rightTokens.size());
        if (ratio >= 0.65) {
            return 34;
        }
        if (ratio >= 0.4) {
            return 22;
        }
        if (intersection > 0) {
            return 10;
        }
        return 0;
    }

    private int amountScore(BigDecimal left, BigDecimal right) {
        if (left == null || right == null || left.signum() <= 0 || right.signum() <= 0) {
            return 0;
        }
        BigDecimal a = left.setScale(2, RoundingMode.HALF_UP).abs();
        BigDecimal b = right.setScale(2, RoundingMode.HALF_UP).abs();
        BigDecimal diff = a.subtract(b).abs();
        if (diff.compareTo(new BigDecimal("0.01")) <= 0) {
            return 35;
        }
        if (diff.compareTo(new BigDecimal("5.00")) <= 0) {
            return 28;
        }
        BigDecimal base = b.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : b;
        BigDecimal pct = diff.divide(base, 6, RoundingMode.HALF_UP);
        if (pct.compareTo(new BigDecimal("0.05")) <= 0) {
            return 22;
        }
        if (pct.compareTo(new BigDecimal("0.15")) <= 0) {
            return 12;
        }
        return 0;
    }

    private int dateScore(LocalDate left, LocalDate right) {
        if (left == null || right == null) {
            return 0;
        }
        long days = Math.abs(ChronoUnit.DAYS.between(left, right));
        if (days == 0) {
            return 15;
        }
        if (days <= 3) {
            return 10;
        }
        if (days <= 7) {
            return 5;
        }
        return 0;
    }

    private Set<String> tokens(String value) {
        Set<String> result = new HashSet<>();
        for (String token : value.split("\\s+")) {
            if (token.length() >= 3) {
                result.add(token);
            }
        }
        return result;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private BigDecimal remainingOrExpected(MonthlyPlanItem item) {
        BigDecimal remaining = nullToZero(item.getExpectedAmount()).subtract(nullToZero(item.getActualAmount()));
        if (remaining.signum() > 0) {
            return remaining;
        }
        return nullToZero(item.getExpectedAmount());
    }

    private void ensurePeriodEditable(MonthlyPlanItem item) {
        if (item.getFinancialPeriod().getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Este ciclo financeiro está fechado. Reabra o ciclo antes de alterar associações ou baixas.");
        }
    }

    private void ensureCanOperate(MonthlyPlanItem item) {
        if (item.getStatus() == MonthlyPlanItemStatus.CANCELED) {
            throw new BusinessException("Não é possível dar baixa em um item cancelado.");
        }
        if (item.getStatus() == MonthlyPlanItemStatus.PAID) {
            throw new BusinessException("Este item planejado já está quitado/recebido.");
        }
        ensurePeriodEditable(item);
    }

    private TransactionType parseTransactionType(String typeText) {
        if (typeText == null || typeText.isBlank()) {
            throw new BusinessException("Informe o tipo: INCOME, EXPENSE ou TRANSFER.");
        }
        String normalized = typeText.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "INCOME", "RECEITA", "ENTRADA", "GANHO", "SALARIO", "SALÁRIO" -> TransactionType.INCOME;
            case "EXPENSE", "DESPESA", "SAIDA", "SAÍDA", "GASTO", "PAGAMENTO", "CONTA" -> TransactionType.EXPENSE;
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERÊNCIA" -> TransactionType.TRANSFER;
            default -> TransactionType.valueOf(normalized);
        };
    }

    private MonthlyPlanItemNature parseNatureOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return MonthlyPlanItemNature.VARIABLE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
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

    private LocalDate clampDate(LocalDate date, FinancialPeriod period) {
        if (date.isBefore(period.getStartDate())) {
            return period.getStartDate();
        }
        if (date.isAfter(period.getEndDate())) {
            return period.getEndDate();
        }
        return date;
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

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String appendNote(String note, String extra) {
        if (extra == null || extra.isBlank()) {
            return note;
        }
        if (note == null || note.isBlank()) {
            return extra.trim();
        }
        return note.trim() + "\n" + extra.trim();
    }

    private void validateTransactionOwnership(String ownerEmail, FinancialTransaction transaction) {
        if (!Objects.equals(transaction.getOwner().getEmail().toLowerCase(Locale.ROOT), ownerEmail.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("A transação não pertence ao usuário autenticado.");
        }
    }

    private record MatchResult(MonthlyPlanItem item, int score) {
    }

    private record TransactionMatch(FinancialTransaction transaction, int score) {
    }
}
