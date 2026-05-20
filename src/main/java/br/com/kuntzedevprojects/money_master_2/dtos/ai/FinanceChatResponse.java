package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;

public record FinanceChatResponse(
        String answer,
        Instant answeredAt
) {
    public static FinanceChatResponse of(String answer) {
        return new FinanceChatResponse(answer, Instant.now());
    }
}
