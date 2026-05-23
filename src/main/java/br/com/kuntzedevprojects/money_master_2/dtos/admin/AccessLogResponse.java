package br.com.kuntzedevprojects.money_master_2.dtos.admin;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.AccessLog;

public record AccessLogResponse(
        Long id,
        Instant occurredAt,
        String method,
        String path,
        String queryString,
        int statusCode,
        long durationMs,
        String principal,
        String clientIp,
        String userAgent,
        boolean success
) {
    public static AccessLogResponse from(AccessLog log) {
        return new AccessLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getMethod(),
                log.getPath(),
                log.getQueryString(),
                log.getStatusCode(),
                log.getDurationMs(),
                log.getPrincipal(),
                log.getClientIp(),
                log.getUserAgent(),
                log.isSuccess()
        );
    }
}
