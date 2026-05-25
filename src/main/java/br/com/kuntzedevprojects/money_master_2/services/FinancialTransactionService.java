package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionCategoryUpdateResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;

@Service
public class FinancialTransactionService {

    private static final int AUTO_ATTACH_VARIABLE_SCORE = 50;
    private static final int AUTO_ATTACH_FIXED_SCORE = 75;

    private final FinancialTransactionRepository transactionRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final FinancialPeriodService financialPeriodService;

    public FinancialTransactionService(
            FinancialTransactionRepository transactionRepository,
            MonthlyPlanItemRepository planItemRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            FinancialPeriodService financialPeriodService
    ) {
        this.transactionRepository = transactionRepository;
        this.planItemRepository = planItemRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<FinancialTransactionResponse> search(
            String ownerEmail,
            LocalDate from,
            LocalDate to,
            Long accountId,
            Long categoryId,
            TransactionType type,
            Long periodId,
            Long planItemId
    ) {
        validatePeriod(from, to);
        return transactionRepository.search(ownerEmail, from, to, accountId, categoryId, type, periodId, planItemId)
                .stream()
                .map(FinancialTransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinancialTransactionResponse get(String ownerEmail, Long id) {
        return FinancialTransactionResponse.from(findOwnedTransaction(ownerEmail, id));
    }

    @Transactional
    public FinancialTransactionResponse create(String ownerEmail, FinancialTransactionCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        Account account = request.accountId() == null
                ? accountService.getOrCreateDefaultAccount(ownerEmail)
                : accountService.findOwnedAccount(ownerEmail, request.accountId());
        Category category = request.categoryId() == null ? null : categoryService.findAvailableCategory(ownerEmail, request.categoryId());
        FinancialPeriod period = request.financialPeriodId() == null
                ? financialPeriodService.findOrCreateForDate(ownerEmail, request.occurredOn())
                : financialPeriodService.findOwnedPeriod(ownerEmail, request.financialPeriodId());
        MonthlyPlanItem planItem = request.monthlyPlanItemId() == null
                ? null
                : financialPeriodService.findOwnedPlanItem(ownerEmail, request.monthlyPlanItemId());

        if (planItem != null) {
            ensureNotInvoiceChild(planItem);
            period = planItem.getFinancialPeriod();
            if (category == null) {
                category = planItem.getCategory();
            }
            if (planItem.getType() != request.type()) {
                throw new BusinessException("O tipo do lançamento precisa ser igual ao tipo do item planejado.");
            }
        }
        validateCategoryType(category, request.type());

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setOwner(owner);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setFinancialPeriod(period);
        transaction.setMonthlyPlanItem(planItem);
        transaction.setType(request.type());
        transaction.setDescription(normalizeRequired(request.description(), "A descrição é obrigatória."));
        transaction.setAmount(normalizeAmount(request.amount()));
        transaction.setOccurredOn(request.occurredOn());
        transaction.setSource(request.source() == null ? TransactionSource.MANUAL : request.source());
        transaction.setNotes(normalizeNullable(request.notes()));

        FinancialTransaction saved = transactionRepository.save(transaction);
        if (planItem == null) {
            autoAttachToMonthlyPlanItem(ownerEmail, saved);
        }
        financialPeriodService.registerPaymentForPlanItem(saved.getMonthlyPlanItem(), saved);
        return FinancialTransactionResponse.from(saved);
    }

    @Transactional
    public FinancialTransactionResponse update(String ownerEmail, Long id, FinancialTransactionUpdateRequest request) {
        FinancialTransaction transaction = findOwnedTransaction(ownerEmail, id);
        MonthlyPlanItem previousPlanItem = transaction.getMonthlyPlanItem();

        if (request.accountId() != null) {
            transaction.setAccount(accountService.findOwnedAccount(ownerEmail, request.accountId()));
        } else if (transaction.getAccount() == null) {
            transaction.setAccount(accountService.getOrCreateDefaultAccount(ownerEmail));
        }
        if (request.categoryId() != null) {
            Category category = categoryService.findAvailableCategory(ownerEmail, request.categoryId());
            validateCategoryType(category, request.type() == null ? transaction.getType() : request.type());
            transaction.setCategory(category);
        }
        if (request.financialPeriodId() != null) {
            transaction.setFinancialPeriod(financialPeriodService.findOwnedPeriod(ownerEmail, request.financialPeriodId()));
        }
        if (Boolean.TRUE.equals(request.clearMonthlyPlanItem())) {
            transaction.setMonthlyPlanItem(null);
        } else if (request.monthlyPlanItemId() != null) {
            MonthlyPlanItem planItem = financialPeriodService.findOwnedPlanItem(ownerEmail, request.monthlyPlanItemId());
            ensureNotInvoiceChild(planItem);
            TransactionType targetType = request.type() == null ? transaction.getType() : request.type();
            if (planItem.getType() != targetType) {
                throw new BusinessException("O tipo do lançamento precisa ser igual ao tipo do item planejado.");
            }
            if (transaction.getCategory() == null && planItem.getCategory() != null) {
                transaction.setCategory(planItem.getCategory());
            }
            transaction.setMonthlyPlanItem(planItem);
            transaction.setFinancialPeriod(planItem.getFinancialPeriod());
        }
        if (request.type() != null) {
            validateCategoryType(transaction.getCategory(), request.type());
            if (transaction.getMonthlyPlanItem() != null && transaction.getMonthlyPlanItem().getType() != request.type()) {
                throw new BusinessException("O tipo do lançamento precisa ser igual ao tipo do item planejado.");
            }
            transaction.setType(request.type());
        }
        if (request.description() != null && !request.description().isBlank()) {
            transaction.setDescription(request.description().trim());
        }
        if (request.amount() != null) {
            transaction.setAmount(normalizeAmount(request.amount()));
        }
        if (request.occurredOn() != null) {
            transaction.setOccurredOn(request.occurredOn());
        }
        if (request.notes() != null) {
            transaction.setNotes(normalizeNullable(request.notes()));
        }

        MonthlyPlanItem currentPlanItem = transaction.getMonthlyPlanItem();
        financialPeriodService.synchronizePlanItemPayment(currentPlanItem);
        if (previousPlanItem != null && (currentPlanItem == null || !previousPlanItem.getId().equals(currentPlanItem.getId()))) {
            financialPeriodService.synchronizePlanItemPayment(previousPlanItem);
        }

        return FinancialTransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(String ownerEmail, Long id) {
        FinancialTransaction transaction = findOwnedTransaction(ownerEmail, id);
        MonthlyPlanItem planItem = transaction.getMonthlyPlanItem();
        transactionRepository.delete(transaction);
        transactionRepository.flush();
        financialPeriodService.synchronizePlanItemPayment(planItem);
    }

    @Transactional
    public ToolTransactionResponse registerFromAi(
            String ownerEmail,
            String typeText,
            BigDecimal amount,
            String description,
            String occurredOnText,
            String accountName,
            String categoryName,
            String originalMessage,
            String notes
    ) {
        return registerFromAi(ownerEmail, typeText, amount, description, occurredOnText, accountName, categoryName, originalMessage, notes, false);
    }

    @Transactional
    public ToolTransactionResponse registerFromAi(
            String ownerEmail,
            String typeText,
            BigDecimal amount,
            String description,
            String occurredOnText,
            String accountName,
            String categoryName,
            String originalMessage,
            String notes,
            Boolean skipMonthlyPlanAutoAdjustment
    ) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        TransactionType type = parseTransactionType(typeText);
        LocalDate occurredOn = parseDateOrToday(occurredOnText);
        Account account = accountService.resolveForAi(ownerEmail, null, accountName);
        Category category = categoryService.resolveForAi(ownerEmail, null, categoryName, type);

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setOwner(owner);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setFinancialPeriod(financialPeriodService.findOrCreateForDate(ownerEmail, occurredOn));
        transaction.setType(type);
        transaction.setDescription(normalizeRequired(description, "A descrição é obrigatória."));
        transaction.setAmount(normalizeAmount(amount));
        transaction.setOccurredOn(occurredOn);
        transaction.setSource(TransactionSource.AI_CHAT);
        transaction.setAiRawMessage(normalizeNullable(originalMessage));
        transaction.setNotes(normalizeNullable(notes));

        FinancialTransaction saved = transactionRepository.save(transaction);
        if (!Boolean.TRUE.equals(skipMonthlyPlanAutoAdjustment)) {
            autoAttachToMonthlyPlanItem(ownerEmail, saved);
        }
        AccountBalanceResponse balance = accountService.balance(ownerEmail, account.getId());

        return new ToolTransactionResponse(
                saved.getId(),
                saved.getType().name(),
                saved.getDescription(),
                saved.getAmount(),
                saved.getOccurredOn(),
                account.getName(),
                category.getName(),
                balance.currentBalance(),
                "Lançamento financeiro registrado com sucesso."
        );
    }

    @Transactional
    public ToolTransactionCategoryUpdateResponse updateTransactionsCategoryByCategoryNameFromAi(
            String ownerEmail,
            String oldCategoryName,
            String newCategoryName,
            String typeText,
            String fromText,
            String toText,
            String originalMessage
    ) {
        String sourceCategoryName = normalizeRequired(oldCategoryName, "Informe a categoria atual das transações.");
        String targetCategoryName = normalizeRequired(newCategoryName, "Informe a nova categoria das transações.");
        TransactionType type = parseTransactionTypeOrNull(typeText);
        LocalDate from = parseDateOrNull(fromText);
        LocalDate to = parseDateOrNull(toText);
        validatePeriod(from, to);

        List<FinancialTransaction> transactions = transactionRepository.findByOwnerCategoryNameAndTypeAndPeriod(
                ownerEmail, sourceCategoryName, type, from, to
        );

        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma transação encontrada com a categoria informada.");
        }

        TransactionType targetType = type == null ? transactions.get(0).getType() : type;
        boolean hasDifferentTypes = transactions.stream().anyMatch(transaction -> transaction.getType() != targetType);
        if (hasDifferentTypes) {
            throw new BusinessException("Encontrei transações de tipos diferentes. Informe se deseja alterar receitas, despesas ou transferências.");
        }

        Category targetCategory = categoryService.resolveForAi(ownerEmail, null, targetCategoryName, targetType);
        transactions.forEach(transaction -> {
            transaction.setCategory(targetCategory);
            transaction.setAiRawMessage(normalizeNullable(originalMessage));
        });

        return new ToolTransactionCategoryUpdateResponse(
                transactions.size(),
                sourceCategoryName,
                targetCategory.getName(),
                targetType.name(),
                from,
                to,
                transactions.size() == 1
                        ? "Categoria da transação atualizada com sucesso."
                        : "Categorias das transações atualizadas com sucesso."
        );
    }

    @Transactional
    public ToolTransactionCategoryUpdateResponse updateTransactionCategoryByDescriptionAndDateFromAi(
            String ownerEmail,
            String description,
            String occurredOnText,
            String newCategoryName,
            String typeText,
            String originalMessage
    ) {
        String descriptionFilter = normalizeRequired(description, "Informe a descrição da transação.");
        String targetCategoryName = normalizeRequired(newCategoryName, "Informe a nova categoria da transação.");
        LocalDate occurredOn = parseDateOrToday(occurredOnText);
        TransactionType type = parseTransactionTypeOrNull(typeText);

        List<FinancialTransaction> transactions = transactionRepository.findByOwnerDescriptionAndDate(
                ownerEmail, descriptionFilter, occurredOn, type
        );

        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma transação encontrada com a descrição e a data informadas.");
        }
        if (transactions.size() > 1) {
            throw new BusinessException("Encontrei mais de uma transação com essa descrição e data. Informe mais detalhes para evitar alteração incorreta.");
        }

        FinancialTransaction transaction = transactions.get(0);
        Category previousCategory = transaction.getCategory();
        Category targetCategory = categoryService.resolveForAi(ownerEmail, null, targetCategoryName, transaction.getType());
        transaction.setCategory(targetCategory);
        transaction.setAiRawMessage(normalizeNullable(originalMessage));

        return new ToolTransactionCategoryUpdateResponse(
                1,
                previousCategory == null ? "Sem categoria" : previousCategory.getName(),
                targetCategory.getName(),
                transaction.getType().name(),
                occurredOn,
                occurredOn,
                "Categoria da transação atualizada com sucesso."
        );
    }

    @Transactional(readOnly = true)
    public FinancialTransaction findOwnedTransaction(String ownerEmail, Long id) {
        return transactionRepository.findByIdAndOwnerEmailWithDetails(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro não encontrado."));
    }

    private void autoAttachToMonthlyPlanItem(String ownerEmail, FinancialTransaction transaction) {
        if (transaction.getMonthlyPlanItem() != null || transaction.getFinancialPeriod() == null) {
            return;
        }
        List<MonthlyPlanItem> candidates = planItemRepository.findActiveCandidatesByOwnerEmailAndPeriod(
                ownerEmail,
                transaction.getFinancialPeriod().getId(),
                transaction.getType()
        );
        if (isCreditCardPurchase(transaction)) {
            // Fatura manual: compras no cartão não aumentam automaticamente o valor da fatura.
            // Quando houver fatura no ciclo, a projeção mensal ignora essas transações para não duplicar o fluxo de caixa.
            return;
        }

        candidates.stream()
                .map(item -> new PlanItemMatch(item, autoAttachScore(transaction, item)))
                .filter(match -> match.score() >= minimumAutoAttachScore(match.item()))
                .max(Comparator.comparingInt(PlanItemMatch::score))
                .map(PlanItemMatch::item)
                .ifPresent(item -> {
                    transaction.setMonthlyPlanItem(item);
                    if (transaction.getCategory() == null && item.getCategory() != null) {
                        transaction.setCategory(item.getCategory());
                    }
                    financialPeriodService.synchronizePlanItemPayment(item);
                });
    }

    private boolean isCreditCardPurchase(FinancialTransaction transaction) {
        if (transaction.getType() != TransactionType.EXPENSE) {
            return false;
        }
        String categoryName = transaction.getCategory() == null ? "" : transaction.getCategory().getName();
        String accountType = transaction.getAccount() == null || transaction.getAccount().getType() == null ? "" : transaction.getAccount().getType().name();
        return containsCreditCardText(categoryName) || "CREDIT_CARD".equals(accountType);
    }

    private boolean isCreditCardPlanItem(MonthlyPlanItem item) {
        String description = item.getDescription();
        String categoryName = item.getCategory() == null ? "" : item.getCategory().getName();
        return item.getType() == TransactionType.EXPENSE
                && item.getStatus() != MonthlyPlanItemStatus.CANCELED
                && (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_PARENT
                    || item.getNature() == MonthlyPlanItemNature.CREDIT_CARD
                    || containsCreditCardText(description)
                    || containsCreditCardText(categoryName));
    }

    private boolean containsCreditCardText(String value) {
        String normalized = normalizeComparable(value);
        return normalized.contains("cartao credito") || normalized.contains("cartao de credito") || normalized.contains("fatura cartao");
    }

    private int minimumAutoAttachScore(MonthlyPlanItem item) {
        if (item.getNature() == MonthlyPlanItemNature.VARIABLE || item.getNature() == MonthlyPlanItemNature.CREDIT_CARD) {
            return AUTO_ATTACH_VARIABLE_SCORE;
        }
        return AUTO_ATTACH_FIXED_SCORE;
    }

    private int autoAttachScore(FinancialTransaction transaction, MonthlyPlanItem item) {
        if (item.getStatus() == MonthlyPlanItemStatus.CANCELED || item.getType() != transaction.getType()) {
            return 0;
        }
        int score = 0;
        score += descriptionScore(transaction.getDescription(), item.getDescription());
        if (item.getNature() == MonthlyPlanItemNature.FIXED) {
            score += amountScore(transaction.getAmount(), item.getExpectedAmount());
        } else {
            score += 10;
        }
        if (transaction.getCategory() != null && item.getCategory() != null && transaction.getCategory().getId().equals(item.getCategory().getId())) {
            score += (item.getNature() == MonthlyPlanItemNature.VARIABLE || item.getNature() == MonthlyPlanItemNature.CREDIT_CARD) ? 45 : 20;
        }
        if (transaction.getAccount() != null && item.getAccount() != null && transaction.getAccount().getId().equals(item.getAccount().getId())) {
            score += 8;
        }
        if (transaction.getOccurredOn() != null && item.getDueDate() != null) {
            long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(transaction.getOccurredOn(), item.getDueDate()));
            if (days == 0) {
                score += 10;
            } else if (days <= 7) {
                score += 5;
            }
        }
        return Math.min(score, 100);
    }

    private int descriptionScore(String left, String right) {
        String a = normalizeComparable(left);
        String b = normalizeComparable(right);
        if (a.isBlank() || b.isBlank()) {
            return 0;
        }
        if (a.equals(b)) {
            return 35;
        }
        if (a.contains(b) || b.contains(a)) {
            return 28;
        }
        Set<String> leftTokens = tokens(a);
        Set<String> rightTokens = tokens(b);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        long intersection = leftTokens.stream().filter(rightTokens::contains).count();
        if (intersection >= 2) {
            return 20;
        }
        if (intersection == 1) {
            return 10;
        }
        return 0;
    }

    private int amountScore(BigDecimal left, BigDecimal right) {
        if (left == null || right == null || left.signum() <= 0 || right.signum() <= 0) {
            return 0;
        }
        BigDecimal diff = left.abs().subtract(right.abs()).abs();
        if (diff.compareTo(new BigDecimal("0.01")) <= 0) {
            return 30;
        }
        if (diff.compareTo(new BigDecimal("5.00")) <= 0) {
            return 22;
        }
        BigDecimal pct = diff.divide(right.abs(), 6, java.math.RoundingMode.HALF_UP);
        if (pct.compareTo(new BigDecimal("0.05")) <= 0) {
            return 15;
        }
        return 0;
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

    private Set<String> tokens(String value) {
        Set<String> tokens = new HashSet<>();
        for (String token : value.split("\\s+")) {
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private TransactionType parseTransactionType(String typeText) {
        if (typeText == null || typeText.isBlank()) {
            throw new BusinessException("O tipo da transação é obrigatório.");
        }
        String normalized = typeText.trim().toUpperCase();
        return switch (normalized) {
            case "INCOME", "RECEITA", "ENTRADA", "GANHO", "SALARIO", "SALÁRIO" -> TransactionType.INCOME;
            case "EXPENSE", "DESPESA", "SAIDA", "SAÍDA", "GASTO", "PAGAMENTO" -> TransactionType.EXPENSE;
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERÊNCIA" -> TransactionType.TRANSFER;
            default -> TransactionType.valueOf(normalized);
        };
    }

    private TransactionType parseTransactionTypeOrNull(String typeText) {
        if (typeText == null || typeText.isBlank()) {
            return null;
        }
        return parseTransactionType(typeText);
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BusinessException("A data deve estar no formato ISO yyyy-MM-dd.");
        }
    }

    private LocalDate parseDateOrToday(String occurredOnText) {
        if (occurredOnText == null || occurredOnText.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(occurredOnText.trim());
        } catch (Exception ex) {
            throw new BusinessException("A data do lançamento deve estar no formato ISO yyyy-MM-dd.");
        }
    }

    private void ensureNotInvoiceChild(MonthlyPlanItem item) {
        if (item.getAggregationType() == MonthlyPlanItemAggregationType.GROUP_CHILD && item.getParentItem() != null) {
            throw new BusinessException("Esta parcela está vinculada à fatura \"" + item.getParentItem().getDescription() + "\". Registre a baixa na fatura principal ou desvincule a parcela antes de associar uma transação individual.");
        }
    }

    private void validateCategoryType(Category category, TransactionType type) {
        if (category != null && category.getType() != type) {
            throw new BusinessException("A categoria informada não pertence ao tipo da transação.");
        }
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("O valor deve ser maior que zero.");
        }
        return amount;
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

    private record PlanItemMatch(MonthlyPlanItem item, int score) {
    }
}
