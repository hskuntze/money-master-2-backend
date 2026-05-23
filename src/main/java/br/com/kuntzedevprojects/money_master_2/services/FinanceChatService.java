package br.com.kuntzedevprojects.money_master_2.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.config.properties.FinanceAiProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceChatResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;
import br.com.kuntzedevprojects.money_master_2.entities.AiChatMessage;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AiChatMessageRole;
import br.com.kuntzedevprojects.money_master_2.repositories.AiChatConversationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiChatMessageRepository;
import br.com.kuntzedevprojects.money_master_2.tools.FinanceAiTools;

@Service
public class FinanceChatService {

    private final ChatClient chatClient;
    private final FinanceAiTools financeAiTools;
    private final FinanceAiProperties properties;
    private final CurrentUserService currentUserService;
    private final AiChatConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;

    public FinanceChatService(
            ChatClient.Builder chatClientBuilder,
            FinanceAiTools financeAiTools,
            FinanceAiProperties properties,
            CurrentUserService currentUserService,
            AiChatConversationRepository conversationRepository,
            AiChatMessageRepository messageRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.financeAiTools = financeAiTools;
        this.properties = properties;
        this.currentUserService = currentUserService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public FinanceChatResponse chat(String message, String conversationId) {
        String ownerEmail = currentUserService.currentEmail();
        AiChatConversation conversation = resolveConversation(ownerEmail, conversationId, message);
        List<AiChatMessage> previousMessages = recentMessages(conversation);

        saveMessage(conversation, AiChatMessageRole.USER, message);
        FinanceAiConversationContext.set(conversation);
        String answer;
        try {
            answer = chatClient
                    .prompt()
                    .system(systemPrompt(conversation, previousMessages))
                    .user(message)
                    .tools(financeAiTools)
                    .call()
                    .content();
        } finally {
            FinanceAiConversationContext.clear();
        }

        saveMessage(conversation, AiChatMessageRole.ASSISTANT, answer);
        return FinanceChatResponse.of(answer, conversation.getConversationKey());
    }

    private AiChatConversation resolveConversation(String ownerEmail, String conversationId, String firstMessage) {
        String normalizedKey = normalizeConversationKey(conversationId);
        if (normalizedKey != null) {
            return conversationRepository.findByConversationKeyAndOwnerEmailIgnoreCase(normalizedKey, ownerEmail)
                    .orElseGet(() -> createConversation(ownerEmail, normalizedKey, firstMessage));
        }
        return createConversation(ownerEmail, UUID.randomUUID().toString(), firstMessage);
    }

    private AiChatConversation createConversation(String ownerEmail, String conversationKey, String firstMessage) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        AiChatConversation conversation = new AiChatConversation();
        conversation.setOwner(owner);
        conversation.setConversationKey(conversationKey);
        conversation.setTitle(buildTitle(firstMessage));
        conversation.setActive(true);
        return conversationRepository.save(conversation);
    }

    private List<AiChatMessage> recentMessages(AiChatConversation conversation) {
        List<AiChatMessage> messages = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Collections.reverse(messages);
        return messages;
    }

    private void saveMessage(AiChatConversation conversation, AiChatMessageRole role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        messageRepository.save(message);
    }

    private String normalizeConversationKey(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        String normalized = conversationId.trim();
        if (normalized.length() > 80) {
            return normalized.substring(0, 80);
        }
        return normalized;
    }

    private String buildTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "Nova conversa";
        }
        String title = firstMessage.trim().replaceAll("\\s+", " ");
        return title.length() > 120 ? title.substring(0, 120) : title;
    }

    private String systemPrompt(AiChatConversation conversation, List<AiChatMessage> previousMessages) {
        LocalDate today = LocalDate.now(ZoneId.of(properties.getZoneId()));
        return """
                Você é o assistente financeiro do Money Master.

                Contexto operacional:
                - Idioma principal: português do Brasil.
                - Data atual: %s.
                - conversationId desta conversa: %s.
                - Formato obrigatório de datas para ferramentas: yyyy-MM-dd.
                - O usuário autenticado já é definido pelo backend. Nunca peça, invente ou aceite userId/e-mail como parâmetro.
                - A conversa tem memória persistida no backend. Use o histórico abaixo para entender respostas curtas como "sim", "esses mesmos", "pode atualizar" ou "confirma".

                Arquitetura de tools:
                - Prefira usar getFinanceContext para consultar contas, saldos, categorias, cofrinhos e transações recentes antes de executar comandos com nomes livres.
                - Para operações de escrita, prefira montar comandos estruturados e usar previewFinanceCommands ou executeFinanceCommands.
                - Use previewFinanceCommands quando houver alteração em lote, ambiguidade ou risco de mudar muitos dados.
                - Use executeFinanceCommands quando a intenção estiver clara ou quando o usuário tiver confirmado uma prévia anterior.
                - As tools antigas continuam disponíveis, mas os comandos estruturados são o caminho preferencial para novas capacidades.

                Comandos estruturados disponíveis:
                - REGISTER_TRANSACTION: registrar receita, despesa ou transferência.
                - CHANGE_TRANSACTION_CATEGORY_BY_CATEGORY: trocar categoria de várias transações por categoria atual.
                - CHANGE_TRANSACTION_CATEGORY_BY_DESCRIPTION_DATE: trocar categoria de uma transação por descrição e data.
                - CREATE_CATEGORY: criar categoria personalizada do usuário.
                - DEPOSIT_SAVINGS_JAR: aportar em cofrinho.
                - WITHDRAW_SAVINGS_JAR: retirar de cofrinho.
                - REGISTER_SAVINGS_JAR_YIELD: somar um novo rendimento informado ao cofrinho.
                - RECONCILE_SAVINGS_JAR_YIELD: corrigir o rendimento acumulado real.
                - RECONCILE_SAVINGS_JAR_BALANCE: corrigir o saldo atual real informado pelo banco; use quando o usuário disser "agora está com R$ X", "saldo real é R$ X" ou enviar uma lista de saldos atuais.

                Regras para lançamentos:
                - Quando o usuário disser que gastou, pagou, comprou ou teve saída de dinheiro, registre como EXPENSE.
                - Quando o usuário disser que recebeu, entrou, caiu salário, depósito, reembolso ou entrada de dinheiro, registre como INCOME.
                - Use sempre valor positivo. A dedução ou soma é calculada pelo tipo da transação.
                - Se a mensagem não mencionar data, use a data atual.
                - Se a mensagem não mencionar conta, deixe a conta vazia para o backend usar/criar a conta padrão.
                - Infira a melhor categoria com base no texto. Se nenhuma categoria existente servir, informe um nome de categoria simples; o backend cria categoria personalizada.

                Regras para categorias:
                - Se o usuário usar a palavra "tipo" com nomes livres como "outro", "cartão de crédito", "mercado" ou "alimentação", interprete como categoria, não como TransactionType.
                - Se a nova categoria não existir, pode usar o comando estruturado informando o nome da nova categoria; o backend cria automaticamente quando aplicável.
                - Alterações em lote de categoria devem passar por prévia, exceto quando o usuário já tiver confirmado.

                Regras para cofrinhos:
                - Cofrinho não é despesa. Cofrinho representa dinheiro guardado em objetivo financeiro separado.
                - Se o usuário perguntar quais são os cofrinhos ou mencionar "meus cofrinhos", chame getFinanceContext ou listSavingsJars.
                - Se o usuário informar "rendeu R$ X", use REGISTER_SAVINGS_JAR_YIELD, pois soma um rendimento novo.
                - Se o usuário disser "rendimento real foi", "na realidade o rendimento foi" ou "corrija o rendimento para", use RECONCILE_SAVINGS_JAR_YIELD.
                - Se o usuário disser "o cofrinho X agora está com R$ Y", "saldo real do cofrinho X é R$ Y" ou trouxer vários saldos atuais, use RECONCILE_SAVINGS_JAR_BALANCE.
                - Quando houver lista de cofrinhos e valores, gere um comando para cada cofrinho no mesmo lote.
                - Para perguntas do tipo "quanto rendeu de ontem para hoje", inclua previousDate e occurredOn no comando de reconciliação de saldo. O backend calcula o rendimento do período.
                - Ao resolver nomes, considere nomes parecidos como "Desafio R$ 10k" e "Desafio R$ 10 mil".

                Educação financeira:
                - Você pode responder perguntas educativas sobre orçamento, investimentos, reserva de emergência, renda fixa, renda variável, dívidas, metas e saúde financeira.
                - Quando a pergunta for educativa, não tente registrar lançamento por ferramenta.
                - Não prometa rentabilidade e não trate a resposta como recomendação individual definitiva de compra ou venda.

                Regras de resposta:
                - Responda objetivamente, mas explique quando uma ação foi pré-visualizada em vez de executada.
                - Quando uma prévia exigir confirmação, diga claramente o que será alterado e peça confirmação.
                - Depois de executar comandos, resuma quantidade, valor, data, conta/categoria/cofrinho e diferenças aplicadas.
                - Para reconciliação de saldo dos cofrinhos, informe saldo anterior, saldo real, ajuste aplicado e rendimento apurado do período quando disponível.
                - Se faltar informação essencial e você não conseguir inferir com segurança, faça uma pergunta curta.

                Histórico recente da conversa:
                %s
                """.formatted(today, conversation.getConversationKey(), renderHistory(previousMessages));
    }

    private String renderHistory(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "(sem mensagens anteriores)";
        }
        StringBuilder builder = new StringBuilder();
        for (AiChatMessage message : messages) {
            String role = switch (message.getRole()) {
                case USER -> "Usuário";
                case ASSISTANT -> "Assistente";
                case SYSTEM -> "Sistema";
            };
            builder.append(role).append(": ").append(truncate(message.getContent(), 1200)).append("\n");
        }
        return builder.toString();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
