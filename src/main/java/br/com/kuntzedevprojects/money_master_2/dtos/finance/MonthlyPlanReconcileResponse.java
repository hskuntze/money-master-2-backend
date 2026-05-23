package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.time.Instant;
import java.util.List;

public record MonthlyPlanReconcileResponse(
        boolean dryRun,
        Long periodId,
        String periodName,
        int analyzedTransactions,
        int matchedTransactions,
        int createdPlanItems,
        int linkedTransactions,
        int ambiguousTransactions,
        int ignoredTransactions,
        List<MonthlyPlanReconcileCandidateResponse> candidates,
        String message,
        Instant processedAt
) {
}
