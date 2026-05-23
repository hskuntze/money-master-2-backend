package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.util.Map;

import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;

public record FinanceCommandResult(
        FinanceCommandType type,
        AiCommandStatus status,
        String message,
        boolean requiresConfirmation,
        Map<String, Object> details
) {
}
