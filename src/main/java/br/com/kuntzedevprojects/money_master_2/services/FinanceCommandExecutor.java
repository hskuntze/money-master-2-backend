package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandItem;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandResult;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceContextResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolSavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionCategoryUpdateResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarBalanceCorrectionResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;

@Service
public class FinanceCommandExecutor {

    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final SavingsJarService savingsJarService;
    private final FinancialTransactionService transactionService;
    private final FinancialReportService reportService;
    private final FinancialTransactionRepository transactionRepository;
    private final AiCommandAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public FinanceCommandExecutor(
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            SavingsJarService savingsJarService,
            FinancialTransactionService transactionService,
            FinancialReportService reportService,
            FinancialTransactionRepository transactionRepository,
            AiCommandAuditRepository auditRepository,
            ObjectMapper objectMapper
    ) {
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.savingsJarService = savingsJarService;
        this.transactionService = transactionService;
        this.reportService = reportService;
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public FinanceContextResponse getContext(String ownerEmail, String fromText, String toText, Boolean includeRecentTransactions) {
        LocalDate from = parseDateOrNull(fromText);
        LocalDate to = parseDateOrNull(toText);
        List<FinancialTransactionResponse> transactions = List.of();
        if (Boolean.TRUE.equals(includeRecentTransactions)) {
            LocalDate effectiveTo = to == null ? LocalDate.now() : to;
            LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(30) : from;
            transactions = transactionService.search(ownerEmail, effectiveFrom, effectiveTo, null, null, null)
                    .stream()
                    .limit(50)
                    .toList();
            from = effectiveFrom;
            to = effectiveTo;
        }
        return new FinanceContextResponse(
                from,
                to,
                accountService.list(ownerEmail),
                reportService.accountBalances(ownerEmail),
                categoryService.listAvailable(ownerEmail, null),
                savingsJarService.list(ownerEmail),
                transactions,
                Instant.now()
        );
    }

    @Transactional
    public FinanceCommandBatchResponse preview(String ownerEmail, FinanceCommandBatchRequest request) {
        FinanceCommandBatchRequest previewRequest = new FinanceCommandBatchRequest(true, request == null ? null : request.reason(), request == null ? null : request.commands());
        return process(ownerEmail, previewRequest);
    }

    @Transactional
    public FinanceCommandBatchResponse execute(String ownerEmail, FinanceCommandBatchRequest request) {
        FinanceCommandBatchRequest executeRequest = new FinanceCommandBatchRequest(false, request == null ? null : request.reason(), request == null ? null : request.commands());
        return process(ownerEmail, executeRequest);
    }

    private FinanceCommandBatchResponse process(String ownerEmail, FinanceCommandBatchRequest request) {
        if (request == null || request.commands() == null || request.commands().isEmpty()) {
            throw new BusinessException("Nenhum comando financeiro foi informado.");
        }
        boolean dryRun = request.isDryRun();
        List<FinanceCommandResult> results = new ArrayList<>();
        for (FinanceCommandItem command : request.commands()) {
            results.add(processOne(ownerEmail, command, dryRun));
        }
        boolean requiresConfirmation = dryRun && results.stream().anyMatch(FinanceCommandResult::requiresConfirmation);
        long successCount = results.stream().filter(result -> result.status() == AiCommandStatus.EXECUTED || result.status() == AiCommandStatus.PREVIEWED).count();
        String summary = dryRun
                ? "Prévia gerada para " + successCount + " comando(s)." + (requiresConfirmation ? " Confirme para executar." : "")
                : "Execução concluída para " + successCount + " comando(s).";
        return new FinanceCommandBatchResponse(dryRun, !dryRun, requiresConfirmation, summary, results, Instant.now());
    }

    private FinanceCommandResult processOne(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (command == null || command.type() == null) {
            throw new BusinessException("Todo comando financeiro precisa ter um tipo.");
        }
        try {
            FinanceCommandResult result = switch (command.type()) {
                case REGISTER_TRANSACTION -> registerTransaction(ownerEmail, command, dryRun);
                case CHANGE_TRANSACTION_CATEGORY_BY_CATEGORY -> changeCategoryByCategory(ownerEmail, command, dryRun);
                case CHANGE_TRANSACTION_CATEGORY_BY_DESCRIPTION_DATE -> changeCategoryByDescriptionAndDate(ownerEmail, command, dryRun);
                case CREATE_CATEGORY -> createCategory(ownerEmail, command, dryRun);
                case DEPOSIT_SAVINGS_JAR -> depositSavingsJar(ownerEmail, command, dryRun);
                case WITHDRAW_SAVINGS_JAR -> withdrawSavingsJar(ownerEmail, command, dryRun);
                case REGISTER_SAVINGS_JAR_YIELD -> registerSavingsJarYield(ownerEmail, command, dryRun);
                case RECONCILE_SAVINGS_JAR_YIELD -> reconcileSavingsJarYield(ownerEmail, command, dryRun);
                case RECONCILE_SAVINGS_JAR_BALANCE -> reconcileSavingsJarBalance(ownerEmail, command, dryRun);
            };
            saveAudit(ownerEmail, command, result, dryRun, null);
            return result;
        } catch (RuntimeException ex) {
            FinanceCommandResult failed = new FinanceCommandResult(
                    command.type(),
                    AiCommandStatus.FAILED,
                    ex.getMessage(),
                    false,
                    Map.of()
            );
            saveAudit(ownerEmail, command, failed, dryRun, ex);
            return failed;
        }
    }

    private FinanceCommandResult registerTransaction(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            return previewed(command.type(), true, "Vou registrar um lançamento financeiro.", mapOf(
                    "tipo", command.transactionType(),
                    "valor", command.amount(),
                    "descricao", command.description(),
                    "data", command.occurredOn(),
                    "conta", command.accountName(),
                    "categoria", command.categoryName()
            ));
        }
        ToolTransactionResponse response = transactionService.registerFromAi(
                ownerEmail,
                command.transactionType(),
                command.amount(),
                command.description(),
                command.occurredOn(),
                command.accountName(),
                command.categoryName(),
                originalMessage(command),
                command.notes()
        );
        return executed(command.type(), response.message(), mapOf("transaction", response));
    }

    private FinanceCommandResult changeCategoryByCategory(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        TransactionType type = parseTransactionTypeOrNull(command.transactionType());
        LocalDate from = parseDateOrNull(command.from());
        LocalDate to = parseDateOrNull(command.to());
        if (dryRun) {
            List<FinancialTransaction> matches = transactionRepository.findByOwnerCategoryNameAndTypeAndPeriod(
                    ownerEmail,
                    required(command.oldCategoryName(), "Informe a categoria atual."),
                    type,
                    from,
                    to
            );
            boolean bulk = matches.size() > 1;
            return previewed(command.type(), bulk, "Encontrei " + matches.size() + " transação(ões) para alteração de categoria.", mapOf(
                    "quantidade", matches.size(),
                    "categoriaAtual", command.oldCategoryName(),
                    "novaCategoria", command.newCategoryName(),
                    "tipo", type == null ? null : type.name(),
                    "de", from,
                    "ate", to
            ));
        }
        ToolTransactionCategoryUpdateResponse response = transactionService.updateTransactionsCategoryByCategoryNameFromAi(
                ownerEmail,
                command.oldCategoryName(),
                command.newCategoryName(),
                command.transactionType(),
                command.from(),
                command.to(),
                originalMessage(command)
        );
        return executed(command.type(), response.message(), mapOf("categoryUpdate", response));
    }

    private FinanceCommandResult changeCategoryByDescriptionAndDate(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            TransactionType type = parseTransactionTypeOrNull(command.transactionType());
            LocalDate occurredOn = parseDateOrToday(command.occurredOn());
            List<FinancialTransaction> matches = transactionRepository.findByOwnerDescriptionAndDate(
                    ownerEmail,
                    required(command.description(), "Informe a descrição da transação."),
                    occurredOn,
                    type
            );
            return previewed(command.type(), matches.size() != 1, "Encontrei " + matches.size() + " transação(ões) com essa descrição e data.", mapOf(
                    "quantidade", matches.size(),
                    "descricao", command.description(),
                    "data", occurredOn,
                    "novaCategoria", command.newCategoryName()
            ));
        }
        ToolTransactionCategoryUpdateResponse response = transactionService.updateTransactionCategoryByDescriptionAndDateFromAi(
                ownerEmail,
                command.description(),
                command.occurredOn(),
                command.newCategoryName(),
                command.transactionType(),
                originalMessage(command)
        );
        return executed(command.type(), response.message(), mapOf("categoryUpdate", response));
    }

    private FinanceCommandResult createCategory(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        TransactionType type = parseTransactionType(command.transactionType());
        if (dryRun) {
            return previewed(command.type(), false, "Vou criar uma categoria personalizada.", mapOf(
                    "nome", categoryName(command),
                    "tipo", type.name()
            ));
        }
        CategoryResponse response = categoryService.create(ownerEmail, new CategoryCreateRequest(categoryName(command), type, null, null, true));
        return executed(command.type(), "Categoria criada com sucesso.", mapOf("category", response));
    }

    private FinanceCommandResult depositSavingsJar(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            var jar = savingsJarService.resolveForAi(ownerEmail, command.savingsJarName(), command.institutionName());
            return previewed(command.type(), false, "Vou registrar um aporte no cofrinho.", mapOf("cofrinho", jar.getName(), "instituicao", jar.getInstitutionName(), "valor", command.amount(), "data", command.occurredOn()));
        }
        ToolSavingsJarResponse response = savingsJarService.depositFromAi(ownerEmail, command.savingsJarName(), command.institutionName(), command.amount(), command.occurredOn(), originalMessage(command));
        return executed(command.type(), response.message(), mapOf("savingsJar", response));
    }

    private FinanceCommandResult withdrawSavingsJar(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            var jar = savingsJarService.resolveForAi(ownerEmail, command.savingsJarName(), command.institutionName());
            return previewed(command.type(), true, "Vou registrar uma retirada do cofrinho.", mapOf("cofrinho", jar.getName(), "instituicao", jar.getInstitutionName(), "valor", command.amount(), "data", command.occurredOn()));
        }
        ToolSavingsJarResponse response = savingsJarService.withdrawFromAi(ownerEmail, command.savingsJarName(), command.institutionName(), command.amount(), command.occurredOn(), originalMessage(command));
        return executed(command.type(), response.message(), mapOf("savingsJar", response));
    }

    private FinanceCommandResult registerSavingsJarYield(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            return previewed(command.type(), false, "Vou somar um rendimento informado ao cofrinho.", mapOf(
                    "cofrinho", command.savingsJarName(),
                    "valor", command.amount(),
                    "data", command.occurredOn()
            ));
        }
        ToolSavingsJarResponse response = savingsJarService.registerYieldFromAi(ownerEmail, command.savingsJarName(), command.institutionName(), command.amount(), command.occurredOn(), originalMessage(command));
        return executed(command.type(), response.message(), mapOf("savingsJar", response));
    }

    private FinanceCommandResult reconcileSavingsJarYield(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            return previewed(command.type(), false, "Vou reconciliar o rendimento acumulado real do cofrinho.", mapOf(
                    "cofrinho", command.savingsJarName(),
                    "rendimentoReal", command.realYieldAmount(),
                    "data", command.occurredOn()
            ));
        }
        ToolSavingsJarResponse response = savingsJarService.correctYieldFromAi(ownerEmail, command.savingsJarName(), command.institutionName(), command.realYieldAmount(), command.occurredOn(), originalMessage(command));
        return executed(command.type(), response.message(), mapOf("savingsJar", response));
    }

    private FinanceCommandResult reconcileSavingsJarBalance(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        SavingsJarBalanceCorrectionResponse response = dryRun
                ? savingsJarService.previewCurrentBalanceCorrectionFromAi(ownerEmail, command.savingsJarName(), command.institutionName(), command.realCurrentAmount(), command.occurredOn(), command.previousDate(), originalMessage(command))
                : savingsJarService.correctCurrentBalanceFromAi(ownerEmail, command.savingsJarName(), command.institutionName(), command.realCurrentAmount(), command.occurredOn(), command.previousDate(), originalMessage(command));
        return dryRun
                ? previewed(command.type(), false, response.message(), mapOf("balanceCorrection", response))
                : executed(command.type(), response.message(), mapOf("balanceCorrection", response));
    }

    private FinanceCommandResult previewed(FinanceCommandType type, boolean requiresConfirmation, String message, Map<String, Object> details) {
        return new FinanceCommandResult(type, AiCommandStatus.PREVIEWED, message, requiresConfirmation, details);
    }

    private FinanceCommandResult executed(FinanceCommandType type, String message, Map<String, Object> details) {
        return new FinanceCommandResult(type, AiCommandStatus.EXECUTED, message, false, details);
    }

    private void saveAudit(String ownerEmail, FinanceCommandItem command, FinanceCommandResult result, boolean dryRun, RuntimeException exception) {
        try {
            User owner = currentUserService.findUserByEmail(ownerEmail);
            AiChatConversation conversation = FinanceAiConversationContext.get();
            AiCommandAudit audit = new AiCommandAudit();
            audit.setOwner(owner);
            audit.setConversation(conversation);
            audit.setCommandType(command.type());
            audit.setStatus(result.status());
            audit.setDryRun(dryRun);
            audit.setCommandJson(writeJson(command));
            audit.setResultJson(writeJson(result));
            audit.setErrorMessage(exception == null ? null : trim(exception.getMessage(), 2000));
            auditRepository.save(audit);
        } catch (Exception ignored) {
            // A auditoria não deve impedir a execução do comando financeiro.
        }
    }

    private String writeJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < entries.length - 1; index += 2) {
            String key = String.valueOf(entries[index]);
            Object value = entries[index + 1];
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    private String categoryName(FinanceCommandItem command) {
        if (command.categoryName() != null && !command.categoryName().isBlank()) {
            return command.categoryName().trim();
        }
        return required(command.newCategoryName(), "Informe o nome da categoria.");
    }

    private String originalMessage(FinanceCommandItem command) {
        String value = command.originalMessage();
        return value == null || value.isBlank() ? "Comando orquestrado pelo chat financeiro." : value;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private TransactionType parseTransactionType(String value) {
        TransactionType type = parseTransactionTypeOrNull(value);
        if (type == null) {
            throw new BusinessException("Informe o tipo da transação/categoria: INCOME, EXPENSE ou TRANSFER.");
        }
        return type;
    }

    private TransactionType parseTransactionTypeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "INCOME", "RECEITA", "ENTRADA", "GANHO", "SALARIO", "SALÁRIO" -> TransactionType.INCOME;
            case "EXPENSE", "DESPESA", "SAIDA", "SAÍDA", "GASTO", "PAGAMENTO" -> TransactionType.EXPENSE;
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERÊNCIA" -> TransactionType.TRANSFER;
            default -> TransactionType.valueOf(normalized);
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

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
