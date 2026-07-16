package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.util.List;

public record FinanceCommandBatchRequest(
        Boolean dryRun,
        String reason,
        String confirmationToken,
        List<FinanceCommandItem> commands
) {
    public boolean isDryRun() {
        return dryRun == null || dryRun;
    }
}
