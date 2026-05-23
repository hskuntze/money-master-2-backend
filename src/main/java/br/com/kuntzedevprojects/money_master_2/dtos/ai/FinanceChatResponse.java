package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;

public record FinanceChatResponse(
        String answer,
        String conversationId,
        Instant answeredAt
) {
    public static FinanceChatResponse of(String answer, String conversationId) {
        return new FinanceChatResponse(answer, conversationId, Instant.now());
    }
}
