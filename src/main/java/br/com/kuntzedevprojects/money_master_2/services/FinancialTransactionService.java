package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;

@Service
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public FinancialTransactionService(
            FinancialTransactionRepository transactionRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService
    ) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<FinancialTransactionResponse> search(
            String ownerEmail,
            LocalDate from,
            LocalDate to,
            Long accountId,
            Long categoryId,
            TransactionType type
    ) {
        validatePeriod(from, to);
        return transactionRepository.search(ownerEmail, from, to, accountId, categoryId, type)
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
        Account account = accountService.findOwnedAccount(ownerEmail, request.accountId());
        Category category = request.categoryId() == null ? null : categoryService.findAvailableCategory(ownerEmail, request.categoryId());

        validateCategoryType(category, request.type());

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setOwner(owner);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setDescription(normalizeRequired(request.description(), "A descrição é obrigatória."));
        transaction.setAmount(normalizeAmount(request.amount()));
        transaction.setOccurredOn(request.occurredOn());
        transaction.setSource(request.source() == null ? TransactionSource.MANUAL : request.source());
        transaction.setNotes(normalizeNullable(request.notes()));

        return FinancialTransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public FinancialTransactionResponse update(String ownerEmail, Long id, FinancialTransactionUpdateRequest request) {
        FinancialTransaction transaction = findOwnedTransaction(ownerEmail, id);

        if (request.accountId() != null) {
            transaction.setAccount(accountService.findOwnedAccount(ownerEmail, request.accountId()));
        }
        if (request.categoryId() != null) {
            Category category = categoryService.findAvailableCategory(ownerEmail, request.categoryId());
            validateCategoryType(category, request.type() == null ? transaction.getType() : request.type());
            transaction.setCategory(category);
        }
        if (request.type() != null) {
            validateCategoryType(transaction.getCategory(), request.type());
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

        return FinancialTransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(String ownerEmail, Long id) {
        FinancialTransaction transaction = findOwnedTransaction(ownerEmail, id);
        transactionRepository.delete(transaction);
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
        User owner = currentUserService.findUserByEmail(ownerEmail);
        TransactionType type = parseTransactionType(typeText);
        LocalDate occurredOn = parseDateOrToday(occurredOnText);
        Account account = accountService.resolveForAi(ownerEmail, null, accountName);
        Category category = categoryService.resolveForAi(ownerEmail, null, categoryName, type);

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setOwner(owner);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setType(type);
        transaction.setDescription(normalizeRequired(description, "A descrição é obrigatória."));
        transaction.setAmount(normalizeAmount(amount));
        transaction.setOccurredOn(occurredOn);
        transaction.setSource(TransactionSource.AI_CHAT);
        transaction.setAiRawMessage(normalizeNullable(originalMessage));
        transaction.setNotes(normalizeNullable(notes));

        FinancialTransaction saved = transactionRepository.save(transaction);
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
}
