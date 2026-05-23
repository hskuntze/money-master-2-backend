package br.com.kuntzedevprojects.money_master_2.dtos.admin;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.FailureLog;

public record FailureLogDetailResponse(
        Long id,
        Instant occurredAt,
        String method,
        String path,
        String queryString,
        int statusCode,
        String principal,
        String clientIp,
        String userAgent,
        String exceptionClass,
        String message,
        String stackTrace
) {
    public static FailureLogDetailResponse from(FailureLog log) {
        return new FailureLogDetailResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getMethod(),
                log.getPath(),
                log.getQueryString(),
                log.getStatusCode(),
                log.getPrincipal(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getExceptionClass(),
                log.getMessage(),
                log.getStackTrace()
        );
    }
}
