package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAuditReversal;

public record AiCommandAuditReversalEventResponse(
        Long id,
        String referenceType,
        Long referenceId,
        String message,
        String resultJson,
        String notes,
        Instant createdAt
) {
    public static AiCommandAuditReversalEventResponse from(AiCommandAuditReversal reversal) {
        if (reversal == null) {
            return null;
        }
        return new AiCommandAuditReversalEventResponse(
                reversal.getId(),
                reversal.getReferenceType(),
                reversal.getReferenceId(),
                reversal.getMessage(),
                reversal.getResultJson(),
                reversal.getNotes(),
                reversal.getCreatedAt()
        );
    }
}
