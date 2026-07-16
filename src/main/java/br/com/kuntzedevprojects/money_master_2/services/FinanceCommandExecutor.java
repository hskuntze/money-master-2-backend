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
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemInvoiceLinkRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPaymentResultResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationPreviewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarBalanceCorrectionResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.AiPrivacySettings;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemInvoiceContributionMode;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoicePaymentService;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;
import br.com.kuntzedevprojects.money_master_2.services.finance.savings.SavingsJarContributionPlanService;

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
    private final PaymentService paymentService;
    private final CreditCardInvoicePaymentService creditCardInvoicePaymentService;
    private final InstallmentAnticipationService installmentAnticipationService;
    private final SavingsJarContributionPlanService savingsJarContributionPlanService;
    private final FinancialTransactionRepository transactionRepository;
    private final AiCommandAuditRepository auditRepository;
    private final AiCommandConfirmationService confirmationService;
    private final AiPrivacySettingsService privacySettingsService;
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
            PaymentService paymentService,
            CreditCardInvoicePaymentService creditCardInvoicePaymentService,
            InstallmentAnticipationService installmentAnticipationService,
            SavingsJarContributionPlanService savingsJarContributionPlanService,
            FinancialTransactionRepository transactionRepository,
            AiCommandAuditRepository auditRepository,
            AiCommandConfirmationService confirmationService,
            AiPrivacySettingsService privacySettingsService,
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
        this.paymentService = paymentService;
        this.creditCardInvoicePaymentService = creditCardInvoicePaymentService;
        this.installmentAnticipationService = installmentAnticipationService;
        this.savingsJarContributionPlanService = savingsJarContributionPlanService;
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
        this.confirmationService = confirmationService;
        this.privacySettingsService = privacySettingsService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public FinanceContextResponse getContext(String ownerEmail, String fromText, String toText, Boolean includeRecentTransactions) {
        AiPrivacySettings settings = privacySettingsService.requireChatAllowed(ownerEmail);
        LocalDate from = parseDateOrNull(fromText);
        LocalDate to = parseDateOrNull(toText);
        List<FinancialTransactionResponse> transactions = List.of();
        if (Boolean.TRUE.equals(includeRecentTransactions) && settings.isShareRecentTransactions()) {
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
                settings.isShareMonthlySummary() ? reportService.accountBalances(ownerEmail) : List.of(),
                categoryService.listAvailable(ownerEmail, null),
                settings.isShareSavingsGoals() ? savingsJarService.list(ownerEmail) : List.of(),
                transactions,
                Instant.now()
        );
    }

    public FinanceCommandBatchResponse preview(String ownerEmail, FinanceCommandBatchRequest request) {
        FinanceCommandBatchRequest previewRequest = new FinanceCommandBatchRequest(true, request == null ? null : request.reason(), null, request == null ? null : request.commands());
        return process(ownerEmail, previewRequest);
    }

    public FinanceCommandBatchResponse execute(String ownerEmail, FinanceCommandBatchRequest request) {
        FinanceCommandBatchRequest executeRequest = new FinanceCommandBatchRequest(false, request == null ? null : request.reason(), request == null ? null : request.confirmationToken(), request == null ? null : request.commands());
        return process(ownerEmail, executeRequest);
    }

    private FinanceCommandBatchResponse process(String ownerEmail, FinanceCommandBatchRequest request) {
        if (request == null || request.commands() == null || request.commands().isEmpty()) {
            throw new BusinessException("Nenhum comando financeiro foi informado.");
        }
        privacySettingsService.requireWriteAllowed(ownerEmail);
        boolean dryRun = request.isDryRun();
        if (!dryRun && requiresServerConfirmation(request.commands())) {
            try {
                confirmationService.validateAndConsume(ownerEmail, request.commands(), request.confirmationToken());
            } catch (BusinessException ex) {
                return confirmationBlocked(ownerEmail, request.commands(), ex.getMessage());
            }
        }
        List<FinanceCommandResult> results = new ArrayList<>();
        for (FinanceCommandItem command : request.commands()) {
            results.add(processOne(ownerEmail, command, dryRun));
        }
        boolean requiresConfirmation = dryRun && results.stream().anyMatch(result -> result.status() == AiCommandStatus.PREVIEWED && result.requiresConfirmation());
        AiCommandConfirmationService.IssuedConfirmation confirmation = requiresConfirmation
                ? confirmationService.create(ownerEmail, request.commands(), request.reason())
                : null;
        long successCount = results.stream().filter(result -> result.status() == AiCommandStatus.EXECUTED || result.status() == AiCommandStatus.PREVIEWED).count();
        String summary = dryRun
                ? "Prévia gerada para " + successCount + " comando(s)." + (requiresConfirmation ? " Confirme para executar." : "")
                : "Execução concluída para " + successCount + " comando(s).";
        return new FinanceCommandBatchResponse(
                dryRun,
                !dryRun,
                requiresConfirmation,
                summary,
                results,
                confirmation == null ? null : confirmation.token(),
                confirmation == null ? null : confirmation.expiresAt(),
                Instant.now()
        );
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
                case CREATE_SAVINGS_JAR -> createSavingsJar(ownerEmail, command, dryRun);
                case CREATE_SAVINGS_JAR_CONTRIBUTION_PLAN -> createSavingsJarContributionPlan(ownerEmail, command, dryRun);
                case CREATE_MONTHLY_PLAN_ITEM, CREATE_MONTHLY_INCOME_PLAN, CREATE_MONTHLY_PAYABLE -> createMonthlyPlanItem(ownerEmail, command, dryRun);
                case PAY_MONTHLY_PLAN_ITEM -> payMonthlyPlanItem(ownerEmail, command, dryRun);
                case REGISTER_PAYMENT -> registerPayment(ownerEmail, command, dryRun);
                case REGISTER_INCOME_RECEIPT -> registerIncomeReceipt(ownerEmail, command, dryRun);
                case PAY_CREDIT_CARD_INVOICE -> payCreditCardInvoice(ownerEmail, command, dryRun);
                case REOPEN_MONTHLY_PLAN_ITEM -> reopenMonthlyPlanItem(ownerEmail, command, dryRun);
                case INCREASE_MONTHLY_PLAN_ITEM_EXPECTED_AMOUNT -> increaseMonthlyPlanItemExpectedAmount(ownerEmail, command, dryRun);
                case CREATE_INSTALLMENT_MONTHLY_PLAN_ITEMS -> createInstallmentMonthlyPlanItems(ownerEmail, command, dryRun);
                case CREATE_INSTALLMENT_PURCHASE -> createInstallmentPurchase(ownerEmail, command, dryRun);
                case PAY_INSTALLMENT_PURCHASE -> payInstallmentPurchase(ownerEmail, command, dryRun);
                case ANTICIPATE_INSTALLMENTS -> anticipateInstallments(ownerEmail, command, dryRun);
                case LINK_MONTHLY_PLAN_ITEM_TO_INVOICE -> linkMonthlyPlanItemToInvoice(ownerEmail, command, dryRun);
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
                    "contaDestino", command.destinationAccountName(),
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
                command.destinationAccountName(),
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

    private FinanceCommandResult createSavingsJar(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        String name = required(command.savingsJarName(), "Informe o nome do cofrinho.");
        if (dryRun) {
            return previewed(command.type(), true, "Vou criar um cofrinho.", mapOf(
                    "nome", name,
                    "instituicao", command.institutionName(),
                    "meta", command.targetAmount(),
                    "dataAlvo", command.targetDate(),
                    "saldoInicial", command.currentAmount(),
                    "rendimentoAtual", command.currentYieldAmount(),
                    "percentualCdi", command.cdiPercentage()
            ));
        }
        ToolSavingsJarResponse response = savingsJarService.createFromAi(
                ownerEmail,
                name,
                command.institutionName(),
                command.targetAmount(),
                command.targetDate(),
                command.imageUrl(),
                command.currentAmount(),
                command.currentYieldAmount(),
                command.cdiPercentage(),
                originalMessage(command)
        );
        return executed(command.type(), response.message(), mapOf("savingsJar", response));
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

    private FinanceCommandResult createSavingsJarContributionPlan(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        Long cycleId = resolveCycleId(ownerEmail, command);
        Long jarId = command.savingsJarId();
        if (jarId == null) {
            jarId = savingsJarService.resolveForAi(ownerEmail, command.savingsJarName(), command.institutionName()).getId();
        }
        LocalDate dueDate = parseDateOrToday(command.dueDate() == null ? command.occurredOn() : command.dueDate());
        if (dryRun) {
            return previewed(command.type(), true, "Vou criar um aporte planejado de cofrinho no ciclo mensal.", mapOf(
                    "cicloId", cycleId,
                    "cofrinhoId", jarId,
                    "cofrinho", command.savingsJarName(),
                    "valor", command.amount(),
                    "dataPrevista", dueDate,
                    "recorrente", command.recurring(),
                    "limiteRecorrencia", command.recurrenceEndDate()
            ));
        }
        SavingsJarContributionPlanResponse response = savingsJarContributionPlanService.create(
                ownerEmail,
                cycleId,
                jarId,
                new SavingsJarContributionPlanRequest(
                        cycleId,
                        command.amount(),
                        dueDate,
                        command.recurring(),
                        parseDateOrNull(command.recurrenceEndDate()),
                        command.notes()
                )
        );
        return executed(command.type(), "Aporte planejado de cofrinho criado no ciclo mensal.", mapOf("savingsJarContributionPlan", response));
    }


    private FinanceCommandResult createMonthlyPlanItem(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        Long cycleId = resolveCycleId(ownerEmail, command);
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
                cycleId,
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

    private FinanceCommandResult registerPayment(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        Long payableId = firstNonNull(command.monthlyPayableId(), command.monthlyPlanItemId());
        if (dryRun) {
            return previewed(command.type(), true, "Vou registrar pagamento de uma conta mensal usando a entidade Payment.", mapOf(
                    "monthlyPayableId", payableId,
                    "valor", command.amount(),
                    "dataPagamento", firstNonBlank(command.paymentDate(), command.occurredOn()),
                    "contaId", command.accountId(),
                    "criarTransacao", command.createIfMissing()
            ));
        }
        PaymentResponse response = paymentService.registerPayablePayment(
                ownerEmail,
                requiredLong(payableId, "Informe o id da conta mensal."),
                paymentRequest(command)
        );
        return executed(command.type(), "Pagamento registrado com sucesso.", mapOf("payment", response));
    }

    private FinanceCommandResult registerIncomeReceipt(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        Long incomePlanId = firstNonNull(command.monthlyIncomePlanId(), command.monthlyPlanItemId());
        if (dryRun) {
            return previewed(command.type(), true, "Vou registrar recebimento de uma renda planejada usando a entidade Payment.", mapOf(
                    "monthlyIncomePlanId", incomePlanId,
                    "valor", command.amount(),
                    "dataRecebimento", firstNonBlank(command.paymentDate(), command.occurredOn()),
                    "contaId", command.accountId(),
                    "criarTransacao", command.createIfMissing()
            ));
        }
        PaymentResponse response = paymentService.registerIncomeReceipt(
                ownerEmail,
                requiredLong(incomePlanId, "Informe o id da renda planejada."),
                paymentRequest(command)
        );
        return executed(command.type(), "Recebimento registrado com sucesso.", mapOf("payment", response));
    }

    private FinanceCommandResult payCreditCardInvoice(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        if (dryRun) {
            return previewed(command.type(), true, "Vou pagar uma fatura de cartao. O pagamento da fatura gera saida de caixa; compras no cartao nao geram saida imediata.", mapOf(
                    "creditCardInvoiceId", command.creditCardInvoiceId(),
                    "valor", command.amount(),
                    "dataPagamento", firstNonBlank(command.paymentDate(), command.occurredOn()),
                    "contaId", command.accountId(),
                    "criarTransacao", command.createIfMissing()
            ));
        }
        CreditCardInvoiceResponse response = creditCardInvoicePaymentService.pay(
                ownerEmail,
                requiredLong(command.creditCardInvoiceId(), "Informe o id da fatura de cartao."),
                paymentRequest(command)
        );
        return executed(command.type(), "Fatura paga com sucesso.", mapOf("creditCardInvoice", response));
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

    private FinanceCommandResult payInstallmentPurchase(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        String description = firstNonBlank(command.installmentPurchaseDescription(), planDescriptionOrNull(command), command.description());
        if (dryRun) {
            boolean requiresConfirmation = command.installmentPurchaseId() == null && (description == null || description.isBlank());
            return previewed(command.type(), requiresConfirmation, "Vou dar baixa em parcela(s) de uma compra parcelada.", mapOf(
                    "compraParceladaId", command.installmentPurchaseId(),
                    "descricaoCompra", description,
                    "parcelasParaBaixar", command.installmentsToPay(),
                    "totalPagoDesejado", command.targetPaidInstallments(),
                    "dataPagamento", firstNonBlank(command.paymentDate(), command.occurredOn())
            ));
        }
        InstallmentPaymentResultResponse response = installmentPurchaseService.markInstallmentsFromAi(
                ownerEmail,
                command.installmentPurchaseId(),
                description,
                command.installmentsToPay(),
                command.targetPaidInstallments(),
                firstNonBlank(command.paymentDate(), command.occurredOn())
        );
        return executed(command.type(), response.message(), mapOf("installmentPurchase", response.purchase()));
    }

    private FinanceCommandResult anticipateInstallments(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        Long purchaseId = requiredLong(command.installmentPurchaseId(), "Informe o id da compra parcelada.");
        List<Long> installmentIds = command.installmentIds();
        if (dryRun) {
            InstallmentAnticipationPreviewResponse preview = installmentAnticipationService.preview(
                    ownerEmail,
                    purchaseId,
                    installmentIds,
                    command.discountAmount(),
                    command.anticipatedAmount(),
                    command.targetInvoiceId()
            );
            return previewed(command.type(), true, preview.projectedImpact(), mapOf("installmentAnticipationPreview", preview));
        }
        InstallmentAnticipationResponse response = installmentAnticipationService.anticipate(
                ownerEmail,
                purchaseId,
                new InstallmentAnticipationRequest(
                        installmentIds,
                        parseDateOrNull(firstNonBlank(command.paymentDate(), command.occurredOn())),
                        command.targetInvoiceId(),
                        command.accountId(),
                        command.anticipatedAmount(),
                        command.discountAmount(),
                        command.notes(),
                        null
                )
        );
        return executed(command.type(), "Parcelas antecipadas com sucesso.", mapOf("installmentAnticipation", response));
    }

    private FinanceCommandResult linkMonthlyPlanItemToInvoice(String ownerEmail, FinanceCommandItem command, boolean dryRun) {
        MonthlyPlanItemInvoiceContributionMode contributionMode = parseInvoiceContributionMode(command.invoiceContributionMode());
        boolean ambiguous = command.invoiceItemId() == null
                || command.monthlyPlanItemId() == null
                || command.invoiceContributionMode() == null
                || command.invoiceContributionMode().isBlank();
        if (dryRun) {
            return previewed(command.type(), ambiguous, "Vou vincular um item mensal como item interno de uma fatura de cartão.", mapOf(
                    "faturaId", command.invoiceItemId(),
                    "itemPlanejadoId", command.monthlyPlanItemId(),
                    "modo", contributionMode.name(),
                    "efeito", contributionMode == MonthlyPlanItemInvoiceContributionMode.ADDS_TO_INVOICE_TOTAL
                            ? "O valor será adicionado ao total da fatura."
                            : "O item apenas compõe a fatura já informada, sem alterar o total."
            ));
        }
        MonthlyPlanItemResponse response = financialPeriodService.linkPlanItemToInvoice(
                ownerEmail,
                requiredLong(command.invoiceItemId(), "Informe o id da fatura."),
                requiredLong(command.monthlyPlanItemId(), "Informe o id do item que será vinculado à fatura."),
                new MonthlyPlanItemInvoiceLinkRequest(contributionMode)
        );
        String message = contributionMode == MonthlyPlanItemInvoiceContributionMode.ADDS_TO_INVOICE_TOTAL
                ? "Item vinculado à fatura e adicionado ao total do cartão."
                : "Item vinculado como composição da fatura, sem alterar o total do cartão.";
        return executed(command.type(), message, mapOf("invoice", response));
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
        Long periodId = resolveCycleId(ownerEmail, command);
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
        return new FinanceCommandResult(type, AiCommandStatus.PREVIEWED, message, requiresConfirmation || requiresServerConfirmation(type), details);
    }

    private FinanceCommandResult executed(FinanceCommandType type, String message, Map<String, Object> details) {
        return new FinanceCommandResult(type, AiCommandStatus.EXECUTED, message, false, details);
    }

    private boolean requiresServerConfirmation(List<FinanceCommandItem> commands) {
        return commands != null && commands.stream().anyMatch(command -> command != null && requiresServerConfirmation(command.type()));
    }

    private boolean requiresServerConfirmation(FinanceCommandType type) {
        return type != null && type != FinanceCommandType.CREATE_CATEGORY;
    }

    private FinanceCommandBatchResponse confirmationBlocked(String ownerEmail, List<FinanceCommandItem> commands, String message) {
        List<FinanceCommandResult> results = commands.stream()
                .map(command -> new FinanceCommandResult(
                        command == null ? null : command.type(),
                        AiCommandStatus.SKIPPED,
                        message,
                        true,
                        mapOf("reason", "CONFIRMATION_REQUIRED")
                ))
                .toList();
        for (int index = 0; index < commands.size(); index++) {
            FinanceCommandItem command = commands.get(index);
            if (command != null) {
                saveAudit(ownerEmail, command, results.get(index), false, null);
            }
        }
        return new FinanceCommandBatchResponse(
                false,
                false,
                true,
                "Execucao bloqueada: " + message,
                results,
                null,
                null,
                Instant.now()
        );
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


    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private PaymentRequest paymentRequest(FinanceCommandItem command) {
        return new PaymentRequest(
                command.transactionId(),
                command.accountId(),
                command.categoryId(),
                command.amount(),
                parseDateOrNull(firstNonBlank(command.paymentDate(), command.occurredOn())),
                null,
                null,
                command.createIfMissing() == null ? true : command.createIfMissing(),
                command.notes()
        );
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long resolveCycleId(String ownerEmail, FinanceCommandItem command) {
        Long explicit = firstNonNull(command.monthlyCycleId(), command.financialPeriodId());
        if (explicit != null) {
            return explicit;
        }
        LocalDate reference = parseDateOrToday(command.dueDate() == null ? command.occurredOn() : command.dueDate());
        return financialPeriodService.findOrCreateForDate(ownerEmail, reference).getId();
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

    private String planDescriptionOrNull(FinanceCommandItem command) {
        String description = firstNonBlank(command.planItemDescription(), command.description(), command.transactionDescription());
        return description == null || description.isBlank() ? null : description.trim();
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

    private Long requiredLong(Long value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        return value;
    }

    private MonthlyPlanItemInvoiceContributionMode parseInvoiceContributionMode(String value) {
        if (value == null || value.isBlank()) {
            return MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "ADDS_TO_INVOICE_TOTAL", "ADICIONAR_A_FATURA", "ADICIONA_NA_FATURA", "SOMA_NA_FATURA", "SOMAR_NA_FATURA", "NOVO_LANCAMENTO", "NOVO_LANÇAMENTO" -> MonthlyPlanItemInvoiceContributionMode.ADDS_TO_INVOICE_TOTAL;
            case "COMPOSITION_ONLY", "APENAS_COMPOR", "COMPOSICAO", "COMPOSIÇÃO", "INFORMATIVO", "RETROATIVO", "RETROATIVA" -> MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY;
            default -> MonthlyPlanItemInvoiceContributionMode.valueOf(normalized);
        };
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
            case "CREDIT_CARD", "CARTAO", "CARTAO_DE_CREDITO", "CREDITO", "FATURA" -> MonthlyPlanItemNature.CREDIT_CARD;
            case "SAVINGS_JAR", "COFRINHO", "RESERVA", "POUPANCA" -> MonthlyPlanItemNature.SAVINGS_JAR;
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
