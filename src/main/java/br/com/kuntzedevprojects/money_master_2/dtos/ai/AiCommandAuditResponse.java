package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;

public record AiCommandAuditResponse(
        Long id,
        Long conversationId,
        FinanceCommandType commandType,
        AiCommandStatus status,
        boolean dryRun,
        String message,
        boolean requiresConfirmation,
        AiCommandReversalResponse reversal,
        AiCommandAuditReversalEventResponse reversalEvent,
        String commandJson,
        String resultJson,
        String errorMessage,
        Instant createdAt
) {
    public static AiCommandAuditResponse from(AiCommandAudit audit, FinanceCommandResult result) {
        return new AiCommandAuditResponse(
                audit.getId(),
                audit.getConversation() == null ? null : audit.getConversation().getId(),
                audit.getCommandType(),
                audit.getStatus(),
                audit.isDryRun(),
                result == null ? null : result.message(),
                result != null && result.requiresConfirmation(),
                null,
                null,
                audit.getCommandJson(),
                audit.getResultJson(),
                audit.getErrorMessage(),
                audit.getCreatedAt()
        );
    }

    public AiCommandAuditResponse withReversal(AiCommandReversalResponse reversal) {
        return new AiCommandAuditResponse(
                id,
                conversationId,
                commandType,
                status,
                dryRun,
                message,
                requiresConfirmation,
                reversal,
                reversalEvent,
                commandJson,
                resultJson,
                errorMessage,
                createdAt
        );
    }

    public AiCommandAuditResponse withReversalEvent(AiCommandAuditReversalEventResponse reversalEvent) {
        return new AiCommandAuditResponse(
                id,
                conversationId,
                commandType,
                status,
                dryRun,
                message,
                requiresConfirmation,
                reversal,
                reversalEvent,
                commandJson,
                resultJson,
                errorMessage,
                createdAt
        );
    }
}
