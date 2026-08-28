package br.com.kuntzedevprojects.money_master_2.dtos.ai;

public record AiCommandAuditReverseResponse(
        Long auditId,
        String referenceType,
        Long referenceId,
        String message,
        Object result,
        AiCommandAuditReversalEventResponse reversalEvent,
        AiCommandAuditResponse audit
) {
}
