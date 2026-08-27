package br.com.kuntzedevprojects.money_master_2.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.time.Instant;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceContextResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolSavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionCategoryUpdateResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanningContextResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.report.MonthlySemanticReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AiPrivacySettings;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.AiPrivacySettingsService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinanceCommandExecutor;
import br.com.kuntzedevprojects.money_master_2.services.FinancialReportService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.MonthlyPlanReconciliationService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;
import br.com.kuntzedevprojects.money_master_2.services.InstallmentPurchaseService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentProductService;

@Component
public class FinanceAiTools {

    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final FinancialTransactionService transactionService;
    private final FinancialReportService reportService;
    private final SavingsJarService savingsJarService;
    private final FinanceCommandExecutor commandExecutor;
    private final FinancialPeriodService financialPeriodService;
    private final MonthlyPlanReconciliationService reconciliationService;
    private final InstallmentPurchaseService installmentPurchaseService;
    private final InvestmentProductService investmentProductService;
    private final AiPrivacySettingsService privacySettingsService;

    public FinanceAiTools(
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            FinancialTransactionService transactionService,
            FinancialReportService reportService,
            SavingsJarService savingsJarService,
            FinanceCommandExecutor commandExecutor,
            FinancialPeriodService financialPeriodService,
            MonthlyPlanReconciliationService reconciliationService,
            InstallmentPurchaseService installmentPurchaseService,
            InvestmentProductService investmentProductService,
            AiPrivacySettingsService privacySettingsService
    ) {
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.reportService = reportService;
        this.savingsJarService = savingsJarService;
        this.commandExecutor = commandExecutor;
        this.financialPeriodService = financialPeriodService;
        this.reconciliationService = reconciliationService;
        this.installmentPurchaseService = installmentPurchaseService;
        this.investmentProductService = investmentProductService;
        this.privacySettingsService = privacySettingsService;
    }


    @Tool(description = "Obtém um contexto financeiro consolidado do usuário autenticado: contas, saldos, categorias, cofrinhos e opcionalmente as últimas transações de um período. Use antes de montar comandos quando houver nomes livres, referências como 'meus cofrinhos' ou pedidos de atualização em lote.")
    public FinanceContextResponse getFinanceContext(
            @ToolParam(description = "Data inicial no formato yyyy-MM-dd. Pode ficar vazio.") String from,
            @ToolParam(description = "Data final no formato yyyy-MM-dd. Pode ficar vazio.") String to,
            @ToolParam(description = "true para incluir até 50 transações recentes do período ou dos últimos 30 dias.") Boolean includeRecentTransactions
    ) {
        String ownerEmail = currentUserService.currentEmail();
        AiPrivacySettings settings = privacySettingsService.requireChatAllowed(ownerEmail);
        boolean includeTransactions = Boolean.TRUE.equals(includeRecentTransactions) && settings.isShareRecentTransactions();
        return commandExecutor.getContext(ownerEmail, from, to, includeTransactions);
    }

    @Tool(description = "Obtém o contexto da Virada do mês: ciclos financeiros, ciclo selecionado, resumo, contas/rendas planejadas e transações ainda não associadas a item mensal. Use antes de dar baixa, associar transações existentes ou reconciliar despesas fixas/variáveis.")
    public MonthlyPlanningContextResponse getMonthlyPlanningContext(
            @ToolParam(description = "ID do ciclo financeiro. Pode ficar vazio para usar o ciclo atual/da data de referência.") String periodId,
            @ToolParam(description = "Data de referência yyyy-MM-dd. Usada quando periodId estiver vazio. Pode ficar vazio para hoje.") String referenceDate,
            @ToolParam(description = "true para incluir transações do ciclo que ainda não estão associadas a item mensal.") Boolean includeUnlinkedTransactions
    ) {
        String ownerEmail = currentUserService.currentEmail();
        requireMonthlyContext(ownerEmail);
        FinancialPeriodResponse selectedPeriod = resolvePeriodForTool(ownerEmail, periodId, referenceDate);
        List<FinancialPeriodResponse> periods = financialPeriodService.list(ownerEmail);
        MonthlyPeriodSummaryResponse summary = financialPeriodService.summary(ownerEmail, selectedPeriod.id());
        List<MonthlyPlanItemResponse> items = financialPeriodService.listPlanItems(ownerEmail, selectedPeriod.id(), null);
        List<FinancialTransactionResponse> unlinkedTransactions = Boolean.TRUE.equals(includeUnlinkedTransactions)
                ? reconciliationService.listUnlinkedTransactions(ownerEmail, selectedPeriod.id(), null)
                : List.of();
        return new MonthlyPlanningContextResponse(selectedPeriod, periods, summary, items, unlinkedTransactions, Instant.now());
    }


    @Tool(description = "Obtem o relatorio mensal semantico do ciclo: planejamento, realizado, faturas, parcelas, cofrinhos, investimentos, saldos e alertas. Use para responder analises do mes, dashboard, pendencias e impacto financeiro antes de sugerir comandos.")
    public MonthlySemanticReportResponse getMonthlySemanticReport(
            @ToolParam(description = "ID do ciclo financeiro. Pode ficar vazio para usar o ciclo atual/da data de referencia.") String periodId,
            @ToolParam(description = "Data de referencia yyyy-MM-dd. Usada quando periodId estiver vazio. Pode ficar vazio para hoje.") String referenceDate
    ) {
        String ownerEmail = currentUserService.currentEmail();
        AiPrivacySettings settings = requireMonthlyContext(ownerEmail);
        FinancialPeriodResponse selectedPeriod = resolvePeriodForTool(ownerEmail, periodId, referenceDate);
        return reportService.monthlySemantic(ownerEmail, selectedPeriod.id(), settings.isShareInvestmentProducts());
    }

    @Tool(description = "Lista compras parceladas do usuario autenticado, com parcelas, status, quantas parcelas foram pagas e quantas ainda estao pendentes. Use antes de dar baixa em parcelas por linguagem natural quando houver nome livre ou possibilidade de ambiguidade.")
    public List<InstallmentPurchaseResponse> listInstallmentPurchases() {
        String ownerEmail = currentUserService.currentEmail();
        requireMonthlyContext(ownerEmail);
        return installmentPurchaseService.list(ownerEmail);
    }

    @Tool(description = "Lista produtos financeiros/investimentos do usuario autenticado, com saldo atual, aportes, resgates, rendimento, instituicao, conta vinculada e liquidez. Use antes de registrar aporte, resgate, rendimento, reconciliacao de saldo ou aporte planejado por linguagem natural.")
    public List<InvestmentProductResponse> listInvestmentProducts() {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireInvestmentProductsShared(ownerEmail);
        return investmentProductService.list(ownerEmail);
    }

    @Tool(description = "Gera uma prévia de comandos financeiros estruturados sem alterar o banco. Para escritas financeiras sensíveis, o backend retorna confirmationToken e confirmationExpiresAt; executeFinanceCommands só executa se receber esse token com o mesmo lote de comandos.")
    public FinanceCommandBatchResponse previewFinanceCommands(
            @ToolParam(description = "Lote de comandos financeiros estruturados. Use dryRun=true na prévia.") FinanceCommandBatchRequest request
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return commandExecutor.preview(ownerEmail, request);
    }

    @Tool(description = "Executa comandos financeiros estruturados no backend. Para escritas financeiras sensíveis, envie confirmationToken retornado por previewFinanceCommands e mantenha exatamente o mesmo lote de comandos. Sem token válido, o backend bloqueia a execução.")
    public FinanceCommandBatchResponse executeFinanceCommands(
            @ToolParam(description = "Lote de comandos financeiros estruturados. Use dryRun=false para executar.") FinanceCommandBatchRequest request
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return commandExecutor.execute(ownerEmail, request);
    }

    @Tool(description = "Lista as contas financeiras ativas e inativas do usuário autenticado. Use para escolher a conta correta antes de registrar gastos ou receitas quando a mensagem mencionar uma conta.")
    public List<AccountResponse> listUserAccounts() {
        return accountService.list(currentUserService.currentEmail());
    }

    @Tool(description = "Lista as categorias financeiras disponíveis para o usuário autenticado. O tipo deve ser INCOME, EXPENSE ou TRANSFER. Use para inferir a melhor categoria antes de registrar um lançamento.")
    public List<CategoryResponse> listUserCategories(
            @ToolParam(description = "Tipo da categoria: INCOME para receitas, EXPENSE para despesas ou TRANSFER para transferências. Pode ser vazio para listar todas.") String type
    ) {
        TransactionType parsedType = parseTransactionTypeOrNull(type);
        return categoryService.listAvailable(currentUserService.currentEmail(), parsedType);
    }

    public ToolTransactionResponse registerFinancialTransaction(
            @ToolParam(description = "Tipo da transação. Use exatamente INCOME para entrada/receita, EXPENSE para gasto/despesa, TRANSFER para transferência.") String type,
            @ToolParam(description = "Valor positivo da transação. Não use sinal negativo.") BigDecimal amount,
            @ToolParam(description = "Descrição curta e clara do lançamento, em português.") String description,
            @ToolParam(description = "Data do lançamento no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Nome da conta mencionada pelo usuário. Se o usuário não mencionar uma conta, deixe vazio para usar/criar a conta padrão.") String accountName,
            @ToolParam(description = "Nome da conta de destino. Obrigatorio quando type=TRANSFER; vazio para receitas e despesas.") String destinationAccountName,
            @ToolParam(description = "Nome da categoria inferida. Exemplos: Alimentação, Salário, Moradia, Educação, Transporte, Assinaturas, Saúde, Pet, Lazer, Compras Online, Outros.") String categoryName,
            @ToolParam(description = "Mensagem original do usuário que originou o lançamento.") String originalMessage,
            @ToolParam(description = "Observações adicionais relevantes, ou vazio.") String notes
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return transactionService.registerFromAi(
                ownerEmail,
                type,
                amount,
                description,
                occurredOn,
                accountName,
                destinationAccountName,
                categoryName,
                originalMessage,
                notes,
                false
        );
    }

    @Tool(description = "Lista os cofrinhos do usuário autenticado com saldo atual, meta, progresso, rendimento total e previsão de rendimento do dia. Use quando o usuário perguntar sobre cofrinhos, metas, dinheiro guardado ou rendimento dos cofres.")
    public List<SavingsJarResponse> listSavingsJars() {
        String ownerEmail = currentUserService.currentEmail();
        requireSavingsContext(ownerEmail);
        return savingsJarService.list(ownerEmail);
    }

    @Tool(description = "Gera um resumo geral dos cofrinhos do usuário autenticado, incluindo total guardado, meta total, total rendido e progresso médio.")
    public SavingsJarSummaryResponse getSavingsJarSummary() {
        String ownerEmail = currentUserService.currentEmail();
        requireSavingsContext(ownerEmail);
        return savingsJarService.summary(ownerEmail);
    }

    public ToolSavingsJarResponse createSavingsJar(
            @ToolParam(description = "Nome/objetivo do cofrinho. Exemplos: Reserva, Viagem, Carro, Desafio de 10 mil.") String name,
            @ToolParam(description = "Nome do banco ou instituição. Exemplos: Itaú, Nubank, PicPay, Banco do Brasil. Pode ser vazio.") String institutionName,
            @ToolParam(description = "Meta monetária do cofrinho. Se não houver meta, use 0.") BigDecimal targetAmount,
            @ToolParam(description = "Data-alvo no formato yyyy-MM-dd. Se o usuário não informar data, deixe vazio.") String targetDate,
            @ToolParam(description = "URL ou descrição textual da imagem do cofrinho. Pode ser vazio.") String imageUrl,
            @ToolParam(description = "Valor atual já guardado no cofrinho, caso o usuário esteja importando um cofre existente. Se não informar, use 0.") BigDecimal currentAmount,
            @ToolParam(description = "Valor total que o cofrinho já rendeu, caso o usuário informe. Se não informar, use 0.") BigDecimal currentYieldAmount,
            @ToolParam(description = "Percentual do CDI. Para 100% do CDI, use 100. Se não houver rendimento automático, use 0.") BigDecimal cdiPercentage,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return savingsJarService.createFromAi(
                ownerEmail,
                name,
                institutionName,
                targetAmount,
                targetDate,
                imageUrl,
                currentAmount,
                currentYieldAmount,
                cdiPercentage,
                originalMessage
        );
    }

    public ToolSavingsJarResponse depositToSavingsJar(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio, mas ajuda a diferenciar cofrinhos com mesmo nome.") String institutionName,
            @ToolParam(description = "Valor positivo depositado no cofrinho.") BigDecimal amount,
            @ToolParam(description = "Data do aporte no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return savingsJarService.depositFromAi(ownerEmail, name, institutionName, amount, occurredOn, originalMessage);
    }

    public ToolSavingsJarResponse withdrawFromSavingsJar(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio.") String institutionName,
            @ToolParam(description = "Valor positivo retirado do cofrinho.") BigDecimal amount,
            @ToolParam(description = "Data da retirada no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return savingsJarService.withdrawFromAi(ownerEmail, name, institutionName, amount, occurredOn, originalMessage);
    }

    public ToolSavingsJarResponse registerSavingsJarYield(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio.") String institutionName,
            @ToolParam(description = "Valor positivo do rendimento informado.") BigDecimal amount,
            @ToolParam(description = "Data do rendimento no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return savingsJarService.registerYieldFromAi(ownerEmail, name, institutionName, amount, occurredOn, originalMessage);
    }

    public ToolSavingsJarResponse reconcileSavingsJarRealYield(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio.") String institutionName,
            @ToolParam(description = "Rendimento real acumulado informado pelo usuário. Use valor positivo ou zero, sem sinal.") BigDecimal realYieldAmount,
            @ToolParam(description = "Data de referência da correção no formato yyyy-MM-dd. Se o usuário não mencionar data, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return savingsJarService.correctYieldFromAi(ownerEmail, name, institutionName, realYieldAmount, occurredOn, originalMessage);
    }

    public ToolTransactionCategoryUpdateResponse updateTransactionsCategoryByCategoryName(
            @ToolParam(description = "Nome da categoria atual que será substituída. Exemplo: Outros.") String oldCategoryName,
            @ToolParam(description = "Nome da nova categoria. Exemplo: Cartão de crédito.") String newCategoryName,
            @ToolParam(description = "Tipo das transações: INCOME, EXPENSE ou TRANSFER. Se o usuário não informar e todas as transações encontradas forem do mesmo tipo, pode ficar vazio.") String type,
            @ToolParam(description = "Data inicial no formato yyyy-MM-dd. Pode ficar vazio se o usuário não limitar período.") String from,
            @ToolParam(description = "Data final no formato yyyy-MM-dd. Pode ficar vazio se o usuário não limitar período.") String to,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return transactionService.updateTransactionsCategoryByCategoryNameFromAi(
                ownerEmail,
                oldCategoryName,
                newCategoryName,
                type,
                from,
                to,
                originalMessage
        );
    }

    public ToolTransactionCategoryUpdateResponse updateTransactionCategoryByDescriptionAndDate(
            @ToolParam(description = "Trecho da descrição da transação a localizar. Exemplo: mercado, almoço, Uber.") String description,
            @ToolParam(description = "Data da transação no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Nome da nova categoria.") String newCategoryName,
            @ToolParam(description = "Tipo da transação: INCOME, EXPENSE ou TRANSFER. Pode ficar vazio se o usuário não informar.") String type,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        String ownerEmail = currentUserService.currentEmail();
        privacySettingsService.requireWriteAllowed(ownerEmail);
        return transactionService.updateTransactionCategoryByDescriptionAndDateFromAi(
                ownerEmail,
                description,
                occurredOn,
                newCategoryName,
                type,
                originalMessage
        );
    }


    private FinancialPeriodResponse resolvePeriodForTool(String ownerEmail, String periodIdText, String referenceDateText) {
        if (periodIdText != null && !periodIdText.isBlank()) {
            return financialPeriodService.get(ownerEmail, Long.valueOf(periodIdText.trim()));
        }
        LocalDate referenceDate = referenceDateText == null || referenceDateText.isBlank()
                ? LocalDate.now()
                : LocalDate.parse(referenceDateText.trim());
        return FinancialPeriodResponse.from(financialPeriodService.findOrCreateForDate(ownerEmail, referenceDate));
    }

    private AiPrivacySettings requireMonthlyContext(String ownerEmail) {
        AiPrivacySettings settings = privacySettingsService.requireChatAllowed(ownerEmail);
        if (!settings.isShareMonthlySummary()) {
            throw new BusinessException("O resumo mensal nao esta compartilhado com a IA nas configuracoes de Privacidade IA.");
        }
        return settings;
    }

    private void requireSavingsContext(String ownerEmail) {
        AiPrivacySettings settings = privacySettingsService.requireChatAllowed(ownerEmail);
        if (!settings.isShareSavingsGoals()) {
            throw new BusinessException("Cofrinhos e metas nao estao compartilhados com a IA nas configuracoes de Privacidade IA.");
        }
    }

    private TransactionType parseTransactionTypeOrNull(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalized = type.trim().toUpperCase();
        return switch (normalized) {
            case "INCOME", "RECEITA", "ENTRADA", "SALARIO", "SALÁRIO" -> TransactionType.INCOME;
            case "EXPENSE", "DESPESA", "SAIDA", "SAÍDA", "GASTO" -> TransactionType.EXPENSE;
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERÊNCIA" -> TransactionType.TRANSFER;
            default -> TransactionType.valueOf(normalized);
        };
    }

    @Tool(description = "Calcula os saldos atuais de todas as contas do usuário autenticado, considerando saldo inicial + receitas - despesas.")
    public List<AccountBalanceResponse> getCurrentAccountBalances() {
        String ownerEmail = currentUserService.currentEmail();
        requireMonthlyContext(ownerEmail);
        return reportService.accountBalances(ownerEmail);
    }

    @Tool(description = "Gera um resumo financeiro por período. Datas devem estar no formato yyyy-MM-dd. Use para responder perguntas sobre entradas, saídas e resultado líquido de um intervalo.")
    public FinancialSummaryResponse getFinancialSummary(
            @ToolParam(description = "Data inicial no formato yyyy-MM-dd.") String from,
            @ToolParam(description = "Data final no formato yyyy-MM-dd.") String to
    ) {
        String ownerEmail = currentUserService.currentEmail();
        requireMonthlyContext(ownerEmail);
        LocalDate fromDate = from == null || from.isBlank() ? null : LocalDate.parse(from);
        LocalDate toDate = to == null || to.isBlank() ? null : LocalDate.parse(to);
        return reportService.summary(ownerEmail, fromDate, toDate);
    }
}
