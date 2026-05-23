package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;
import java.util.List;

public record FinanceCommandBatchResponse(
        boolean dryRun,
        boolean executed,
        boolean requiresConfirmation,
        String summary,
        List<FinanceCommandResult> results,
        Instant processedAt
) {
}
