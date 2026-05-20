package br.com.kuntzedevprojects.money_master_2.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolSavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialReportService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;

@Component
public class FinanceAiTools {

    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final FinancialTransactionService transactionService;
    private final FinancialReportService reportService;
    private final SavingsJarService savingsJarService;

    public FinanceAiTools(
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            FinancialTransactionService transactionService,
            FinancialReportService reportService,
            SavingsJarService savingsJarService
    ) {
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.reportService = reportService;
        this.savingsJarService = savingsJarService;
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

    @Tool(description = "Registra um lançamento financeiro para o usuário autenticado. Use quando a mensagem indicar gasto, pagamento, compra, receita, salário, depósito ou entrada de dinheiro. Se a categoria não existir, ela será criada automaticamente para o usuário.")
    public ToolTransactionResponse registerFinancialTransaction(
            @ToolParam(description = "Tipo da transação. Use exatamente INCOME para entrada/receita, EXPENSE para gasto/despesa, TRANSFER para transferência.") String type,
            @ToolParam(description = "Valor positivo da transação. Não use sinal negativo.") BigDecimal amount,
            @ToolParam(description = "Descrição curta e clara do lançamento, em português.") String description,
            @ToolParam(description = "Data do lançamento no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Nome da conta mencionada pelo usuário. Se o usuário não mencionar uma conta, deixe vazio para usar/criar a conta padrão.") String accountName,
            @ToolParam(description = "Nome da categoria inferida. Exemplos: Alimentação, Salário, Moradia, Educação, Transporte, Assinaturas, Saúde, Pet, Lazer, Compras Online, Outros.") String categoryName,
            @ToolParam(description = "Mensagem original do usuário que originou o lançamento.") String originalMessage,
            @ToolParam(description = "Observações adicionais relevantes, ou vazio.") String notes
    ) {
        return transactionService.registerFromAi(
                currentUserService.currentEmail(),
                type,
                amount,
                description,
                occurredOn,
                accountName,
                categoryName,
                originalMessage,
                notes
        );
    }

    @Tool(description = "Lista os cofrinhos do usuário autenticado com saldo atual, meta, progresso, rendimento total e previsão de rendimento do dia. Use quando o usuário perguntar sobre cofrinhos, metas, dinheiro guardado ou rendimento dos cofres.")
    public List<SavingsJarResponse> listSavingsJars() {
        return savingsJarService.list(currentUserService.currentEmail());
    }

    @Tool(description = "Gera um resumo geral dos cofrinhos do usuário autenticado, incluindo total guardado, meta total, total rendido e progresso médio.")
    public SavingsJarSummaryResponse getSavingsJarSummary() {
        return savingsJarService.summary(currentUserService.currentEmail());
    }

    @Tool(description = "Cria um novo cofrinho para o usuário autenticado. Use quando o usuário disser que criou ou quer criar um cofrinho em um banco, com objetivo/meta, data-alvo opcional, imagem e rendimento como percentual do CDI.")
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
        return savingsJarService.createFromAi(
                currentUserService.currentEmail(),
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

    @Tool(description = "Registra um aporte/depósito em um cofrinho existente. Use quando o usuário disser que guardou, colocou ou adicionou dinheiro em um cofrinho.")
    public ToolSavingsJarResponse depositToSavingsJar(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio, mas ajuda a diferenciar cofrinhos com mesmo nome.") String institutionName,
            @ToolParam(description = "Valor positivo depositado no cofrinho.") BigDecimal amount,
            @ToolParam(description = "Data do aporte no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        return savingsJarService.depositFromAi(currentUserService.currentEmail(), name, institutionName, amount, occurredOn, originalMessage);
    }

    @Tool(description = "Registra uma retirada de um cofrinho existente. Use quando o usuário disser que retirou, tirou, resgatou ou sacou dinheiro de um cofrinho.")
    public ToolSavingsJarResponse withdrawFromSavingsJar(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio.") String institutionName,
            @ToolParam(description = "Valor positivo retirado do cofrinho.") BigDecimal amount,
            @ToolParam(description = "Data da retirada no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        return savingsJarService.withdrawFromAi(currentUserService.currentEmail(), name, institutionName, amount, occurredOn, originalMessage);
    }

    @Tool(description = "Registra manualmente rendimento em um cofrinho existente. Use quando o usuário disser que o cofrinho rendeu determinado valor no app do banco.")
    public ToolSavingsJarResponse registerSavingsJarYield(
            @ToolParam(description = "Nome do cofrinho.") String name,
            @ToolParam(description = "Banco ou instituição do cofrinho. Pode ser vazio.") String institutionName,
            @ToolParam(description = "Valor positivo do rendimento informado.") BigDecimal amount,
            @ToolParam(description = "Data do rendimento no formato yyyy-MM-dd. Se o usuário disser hoje, use a data atual informada no prompt do sistema.") String occurredOn,
            @ToolParam(description = "Mensagem original do usuário.") String originalMessage
    ) {
        return savingsJarService.registerYieldFromAi(currentUserService.currentEmail(), name, institutionName, amount, occurredOn, originalMessage);
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
        return reportService.accountBalances(currentUserService.currentEmail());
    }

    @Tool(description = "Gera um resumo financeiro por período. Datas devem estar no formato yyyy-MM-dd. Use para responder perguntas sobre entradas, saídas e resultado líquido de um intervalo.")
    public FinancialSummaryResponse getFinancialSummary(
            @ToolParam(description = "Data inicial no formato yyyy-MM-dd.") String from,
            @ToolParam(description = "Data final no formato yyyy-MM-dd.") String to
    ) {
        LocalDate fromDate = from == null || from.isBlank() ? null : LocalDate.parse(from);
        LocalDate toDate = to == null || to.isBlank() ? null : LocalDate.parse(to);
        return reportService.summary(currentUserService.currentEmail(), fromDate, toDate);
    }
}
