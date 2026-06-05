package br.com.kuntzedevprojects.money_master_2.dtos.admin;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.SecurityAuditEvent;

public record SecurityAuditEventResponse(
        Long id,
        Instant occurredAt,
        String eventType,
        String principal,
        String clientIp,
        String userAgent,
        String method,
        String path,
        boolean success,
        String details
) {
    public static SecurityAuditEventResponse from(SecurityAuditEvent event) {
        return new SecurityAuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getEventType(),
                event.getPrincipal(),
                event.getClientIp(),
                event.getUserAgent(),
                event.getMethod(),
                event.getPath(),
                event.isSuccess(),
                event.getDetails()
        );
    }
}
