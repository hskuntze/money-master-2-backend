package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarBalanceCorrectionResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;

@Service
public class FinanceCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FinanceCommandExecutor.class);

    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final SavingsJarService savingsJarService;
    private final FinancialTransactionService transactionService;
    private final FinancialReportService reportService;
    private final FinancialPeriodService financialPeriodService;
    private final MonthlyPlanReconciliationService reconciliationService;
    private final InstallmentPurchaseService installmentPurchaseService;
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
            FinancialPeriodService financialPeriodService,
            MonthlyPlanReconciliationService reconciliationService,
            InstallmentPurchaseService installmentPurchaseService,
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
        this.financialPeriodService = financialPeriodService;
        this.reconciliationService = reconciliationService;
        this.installmentPurchaseService = installmentPurchaseService;
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
            transactions = transactionService.search(ownerEmail, effectiveFrom, effectiveTo, null, null, null, null, null)
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

    public FinanceCommandBatchResponse preview(String ownerEmail, FinanceCommandBatchRequest request) {
        FinanceCommandBatchRequest previewRequest = new FinanceCommandBatchRequest(true, request == null ? null : request.reason(), request == null ? null : request.commands());
        return process(ownerEmail, previewRequest);
    }

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
                case REGISTER_TRANSACTION -> registerTransaction(ownerEmail, command, dryRun, null);
                case INCOME, EXPENSE, TRANSFER -> registerTransaction(ownerEmail, command, dryRun, command.type().impliedTransactionType());
                case CHANGE_TRANSACTION_CATEGORY_BY_CATEGORY -> changeCategoryByCategory(ownerEmail, command, dryRun);
                case CHANGE_TRANSACTION_CATEGORY_BY_DESCRIPTION_DATE -> changeCategoryByDescriptionAndDate(ownerEmail, command, dryRun);
                case CREATE_CATEGORY -> createCategory(ownerEmail, command, dryRun);
                case DEPOSIT_SAVINGS_JAR -> depositSavingsJar(ownerEmail, command, dryRun);
                case WITHDRAW_SAVINGS_JAR -> withdrawSavingsJar(ownerEmail, command, dryRun);
                case REGISTER_SAVINGS_JAR_YIELD -> registerSavingsJarYield(ownerEmail, command, dryRun);
                case RECONCILE_SAVINGS_JAR_YIELD -> reconcileSavingsJarYield(ownerEmail, command, dryRun);
                case RECONCILE_SAVINGS_JAR_BALANCE -> reconcileSavingsJarBalance(ownerEmail, command, dryRun);
                case CREATE_MONTHLY_PLAN_ITEM -> createMonthlyPlanItem(ownerEmail, command, dryRun);
                case PAY_MONTHLY_PLAN_ITEM -> payMonthlyPlanItem(ownerEmail, command, dryRun);
                case REOPEN_MONTHLY_PLAN_ITEM -> reopenMonthlyPlanItem(ownerEmail, command, dryRun);
                case INCREASE_MONTHLY_PLAN_ITEM_EXPECTED_AMOUNT -> increaseMonthlyPlanItemExpectedAmount(ownerEmail, command, dryRun);
                case CREATE_INSTALLMENT_MONTHLY_PLAN_ITEMS -> createInstallmentMonthlyPlanItems(ownerEmail, command, dryRun);
                case CREATE_INSTALLMENT_PURCHASE -> createInstallmentPurchase(ownerEmail, command, dryRun);
                case LINK_TRANSACTION_TO_MONTHLY_PLAN_ITEM -> linkTransactionToMonthlyPlanItem(ownerEmail, command, dryRun);
                case RECONCILE_MONTHLY_PLAN_WITH_TRANSACTIONS -> reconcileMonthlyPlan(ownerEmail, command, dryRun);
            };
            saveAudit(ownerEmail, command, result, dryRun, null);
            return result;
        } catch (RuntimeException ex) {
            logger.warn("Comando financeiro executado pelo chat falhou. ownerEmail={}, type={}, dryRun={}, message={}",
                    ownerEmail, command.type(), dryRun, ex.getMessage(), ex);
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

    private FinanceCommandResult registerTransaction(String ownerEmail, FinanceCommandItem command, boolean dryRun, String impliedTransactionType) {
        String transactionType = firstNonBlank(command.transactionType(), impliedTransactionType);
        if (dryRun) {
            return previewed(FinanceCommandType.REGISTER_TRANSACTION, true, "Vou registrar um lançamento financeiro.", mapOf(
                    "tipo", transactionType,
                    "valor", command.amount(),
                    "descricao", command.description(),
                    "data", command.occurredOn(),
                    "conta", command.accountName(),
                    "categoria", command.categoryName()
            ));
        }
        ToolTransactionResponse response = transactionService.registerFromAi(
                ownerEmail,
                transactionType,
                command.amount(),
                command.description(),
                command.occurredOn(),
                command.accountName(),
                command.categoryName(),
                originalMessage(command),
                command.notes(),
                command.skipMonthlyPlanAutoAdjustment()
        );
        return executed(FinanceCommandType.REGISTER_TRANSACTION, response.message(), mapOf("transaction", response));
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


    private FinanceCommandResult createMonthlyPlanItem(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        LocalDate dueDate = parseDateOrToday(command.dueDate() == null ? command.occurredOn() : command.dueDate());
        String safeDescription = planDescription(command);
        if (dryRun) {
            return previewed(command.type(), false, "Vou criar um item planejado no controle mensal, sem registrar uma transação real.", mapOf(
                    "descricao", safeDescription,
                    "tipo", parseTransactionType(command.transactionType()).name(),
                    "valorPrevisto", command.amount(),
                    "vencimento", dueDate,
                    "natureza", command.planItemNature(),
                    "recorrente", command.recurring()
            ));
        }
        MonthlyPlanItemResponse response = reconciliationService.createPlanItemFromAi(
                ownerEmail,
                command.financialPeriodId(),
                command.transactionType(),
                safeDescription,
                command.amount(),
                dueDate.toString(),
                command.accountName(),
                command.categoryName(),
                command.planItemNature(),
                command.recurring(),
                command.notes()
        );
        return executed(command.type(), "Item do planejamento mensal criado com sucesso.", mapOf("monthlyPlanItem", response));
    }

    private FinanceCommandResult payMonthlyPlanItem(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        LocalDate occurredOn = parseDateOrToday(command.occurredOn() == null ? command.dueDate() : command.occurredOn());
        if (dryRun) {
            return previewed(command.type(), true, "Vou dar baixa em uma conta/renda planejada. Primeiro tentarei associar uma transação já existente; se não houver correspondência segura, criarei uma nova transação de baixa.", mapOf(
                    "itemPlanejadoId", command.monthlyPlanItemId(),
                    "transacaoExistenteId", command.transactionId(),
                    "descricao", planDescription(command),
                    "valor", command.amount(),
                    "data", occurredOn,
                    "criarItemSeNaoExistir", command.createIfMissing(),
                    "preferirTransacaoExistente", command.preferExistingTransaction(),
                    "forcarReassociacao", command.forceRelink()
            ));
        }
        MonthlyPlanItemResponse response = reconciliationService.payPlanItemFromAi(
                ownerEmail,
                command.monthlyPlanItemId(),
                command.transactionId(),
                command.financialPeriodId(),
                command.transactionType(),
                planDescription(command),
                command.amount(),
                occurredOn.toString(),
                command.accountName(),
                command.categoryName(),
                command.planItemNature(),
                command.recurring(),
                command.createIfMissing(),
                command.preferExistingTransaction(),
                originalMessage(command),
                command.notes()
        );
        return executed(command.type(), "Baixa do planejamento mensal processada com sucesso.", mapOf("monthlyPlanItem", response));
    }

    private FinanceCommandResult reopenMonthlyPlanItem(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        LocalDate referenceDate = parseDateOrToday(command.occurredOn() == null ? command.dueDate() : command.occurredOn());
        String safeDescription = command.monthlyPlanItemId() == null ? planDescription(command) : optionalPlanDescription(command);
        if (dryRun) {
            return previewed(command.type(), true, "Vou desfazer a baixa e marcar o item planejado como pendente.", mapOf(
                    "itemPlanejadoId", command.monthlyPlanItemId(),
                    "descricao", safeDescription,
                    "dataReferencia", referenceDate,
                    "excluirTransacoesVinculadas", command.deleteLinkedTransactions(),
                    "observacao", command.notes()
            ));
        }
        MonthlyPlanItemResponse response = reconciliationService.reopenPlanItemFromAi(
                ownerEmail,
                command.monthlyPlanItemId(),
                command.financialPeriodId(),
                command.transactionType(),
                safeDescription,
                command.amount(),
                referenceDate.toString(),
                command.accountName(),
                command.categoryName(),
                command.planItemNature(),
                command.deleteLinkedTransactions(),
                command.createIfMissing(),
                command.notes()
        );
        String message = Boolean.TRUE.equals(command.deleteLinkedTransactions())
                ? "Baixa desfeita. As transações vinculadas foram excluídas e o item voltou para pendente."
                : "Baixa desfeita. As transações vinculadas foram mantidas como lançamentos avulsos e o item voltou para pendente.";
        return executed(command.type(), message, mapOf("monthlyPlanItem", response));
    }

    private FinanceCommandResult increaseMonthlyPlanItemExpectedAmount(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        LocalDate dueDate = parseDateOrToday(command.dueDate() == null ? command.occurredOn() : command.dueDate());
        String safeDescription = planDescription(command);
        if (dryRun) {
            return previewed(command.type(), false, "Vou aumentar o valor previsto de um item do planejamento, sem marcar como pago e sem criar baixa.", mapOf(
                    "descricao", safeDescription,
                    "tipo", parseTransactionType(command.transactionType()).name(),
                    "valorAdicionar", command.amount(),
                    "vencimento", dueDate,
                    "natureza", command.planItemNature(),
                    "recorrente", command.recurring(),
                    "limiteRecorrencia", command.recurrenceEndDate()
            ));
        }
        MonthlyPlanItemResponse response = financialPeriodService.increaseMonthlyPlanItemExpectedAmountFromAi(
                ownerEmail,
                command.transactionType(),
                safeDescription,
                command.amount(),
                dueDate.toString(),
                command.accountName(),
                command.categoryName(),
                command.planItemNature(),
                command.recurring(),
                command.recurrenceEndDate(),
                command.notes()
        );
        return executed(command.type(), "Valor previsto do planejamento atualizado com sucesso, sem baixa/pagamento.", mapOf("monthlyPlanItem", response));
    }

    private FinanceCommandResult createInstallmentMonthlyPlanItems(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        int installments = command.installmentCount() == null || command.installmentCount() <= 0 ? 1 : command.installmentCount();
        LocalDate firstDueDate = parseDateOrToday(command.firstDueDate() == null ? (command.dueDate() == null ? command.occurredOn() : command.dueDate()) : command.firstDueDate());
        String safeDescription = planDescription(command);
        if (dryRun) {
            return previewed(command.type(), installments > 1, "Vou distribuir parcelas no planejamento mensal, criando ciclos futuros quando necessário e apenas aumentando o previsto, sem marcar como pago.", mapOf(
                    "descricao", safeDescription,
                    "tipo", parseTransactionType(command.transactionType()).name(),
                    "valorParcela", command.amount(),
                    "parcelas", installments,
                    "primeiroVencimento", firstDueDate,
                    "categoria", command.categoryName(),
                    "conta", command.accountName()
            ));
        }
        List<MonthlyPlanItemResponse> responses = financialPeriodService.createInstallmentPlanItemsFromAi(
                ownerEmail,
                command.transactionType(),
                safeDescription,
                command.amount(),
                installments,
                firstDueDate.toString(),
                command.accountName(),
                command.categoryName(),
                command.planItemNature(),
                command.recurring(),
                command.recurrenceEndDate(),
                command.notes()
        );
        return executed(command.type(), "Parcelas distribuídas no planejamento mensal com sucesso.", mapOf(
                "parcelasCriadasOuAtualizadas", responses.size(),
                "monthlyPlanItems", responses
        ));
    }


    private FinanceCommandResult createInstallmentPurchase(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            return previewed(command.type(), true, "Vou criar uma compra parcelada e lançar as parcelas nos ciclos mensais.", mapOf(
                    "descricao", planDescription(command),
                    "valorTotal", command.amount(),
                    "valorParcela", command.amount(),
                    "quantidadeParcelas", command.installmentCount(),
                    "primeiraParcela", command.firstDueDate(),
                    "categoria", command.categoryName()
            ));
        }
        InstallmentPurchaseResponse response = installmentPurchaseService.createFromAi(
                ownerEmail,
                planDescription(command),
                null,
                command.amount(),
                command.installmentCount(),
                command.occurredOn(),
                command.firstDueDate(),
                command.categoryName(),
                command.notes()
        );
        return executed(command.type(), "Compra parcelada criada e parcelas lançadas no planejamento mensal.", mapOf("installmentPurchase", response));
    }

    private FinanceCommandResult linkTransactionToMonthlyPlanItem(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        LocalDate occurredOn = parseDateOrToday(command.occurredOn() == null ? command.dueDate() : command.occurredOn());
        if (dryRun) {
            boolean ambiguous = command.monthlyPlanItemId() == null || command.transactionId() == null;
            return previewed(command.type(), ambiguous, "Vou associar uma transação existente a um item planejado, sem criar novo gasto/receita.", mapOf(
                    "itemPlanejadoId", command.monthlyPlanItemId(),
                    "transacaoId", command.transactionId(),
                    "descricaoItem", planDescription(command),
                    "descricaoTransacao", command.transactionDescription(),
                    "valor", command.amount(),
                    "data", occurredOn,
                    "forcarReassociacao", command.forceRelink()
            ));
        }
        MonthlyPlanItemResponse response = reconciliationService.linkTransactionToPlanItemFromAi(
                ownerEmail,
                command.monthlyPlanItemId(),
                command.transactionId(),
                command.financialPeriodId(),
                command.transactionType(),
                planDescription(command),
                command.transactionDescription(),
                command.amount(),
                occurredOn.toString(),
                command.accountName(),
                command.categoryName(),
                command.planItemNature(),
                command.recurring(),
                command.createIfMissing(),
                command.forceRelink(),
                command.notes()
        );
        return executed(command.type(), "Transação associada ao planejamento mensal com sucesso.", mapOf("monthlyPlanItem", response));
    }

    private FinanceCommandResult reconcileMonthlyPlan(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        Long periodId = command.financialPeriodId();
        if (periodId == null) {
            LocalDate reference = parseDateOrToday(command.occurredOn());
            periodId = financialPeriodService.findOrCreateForDate(ownerEmail, reference).getId();
        }
        MonthlyPlanReconcileRequest request = new MonthlyPlanReconcileRequest(
                dryRun,
                command.createMissingPlanItems(),
                command.linkExistingTransactions(),
                command.onlyUnlinkedTransactions(),
                command.defaultNature() == null ? parseNatureOrNull(command.planItemNature()) : command.defaultNature(),
                command.recurring(),
                command.reconcileTransactionType() == null ? parseTransactionTypeOrNull(command.transactionType()) : command.reconcileTransactionType(),
                parseDateOrNull(command.from()),
                parseDateOrNull(command.to()),
                command.createPlanItemsAsPendingOnly(),
                command.deleteSourceTransactionsWhenCreatingPlanItems()
        );
        MonthlyPlanReconcileResponse response = reconciliationService.reconcile(ownerEmail, periodId, request);
        return dryRun
                ? previewed(command.type(), true, response.message(), mapOf("reconciliation", response))
                : executed(command.type(), response.message(), mapOf("reconciliation", response));
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
            audit.setCommandType(result.type() == null ? command.type() : result.type());
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


    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    private String optionalPlanDescription(FinanceCommandItem command) {
        if (command.planItemDescription() != null && !command.planItemDescription().isBlank()) {
            return command.planItemDescription().trim();
        }
        if (command.description() != null && !command.description().isBlank()) {
            return command.description().trim();
        }
        if (command.transactionDescription() != null && !command.transactionDescription().isBlank()) {
            return command.transactionDescription().trim();
        }
        return null;
    }

    private String planDescription(FinanceCommandItem command) {
        if (command.planItemDescription() != null && !command.planItemDescription().isBlank()) {
            return command.planItemDescription().trim();
        }
        if (command.description() != null && !command.description().isBlank()) {
            return command.description().trim();
        }
        return required(command.transactionDescription(), "Informe a descrição do item planejado.");
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

    private MonthlyPlanItemNature parseNatureOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
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

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
