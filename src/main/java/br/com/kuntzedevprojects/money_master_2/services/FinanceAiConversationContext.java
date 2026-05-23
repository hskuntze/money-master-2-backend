package br.com.kuntzedevprojects.money_master_2.services;

import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;

public final class FinanceAiConversationContext {

    private static final ThreadLocal<AiChatConversation> CURRENT = new ThreadLocal<>();

    private FinanceAiConversationContext() {
    }

    public static void set(AiChatConversation conversation) {
        CURRENT.set(conversation);
    }

    public static AiChatConversation get() {
        return CURRENT.get();
    }

    public static String conversationKeyOrNull() {
        AiChatConversation conversation = CURRENT.get();
        return conversation == null ? null : conversation.getConversationKey();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
