package br.com.kuntzedevprojects.money_master_2.dtos.admin;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.services.EmailSettingsService.ResolvedEmailSettings;

public record EmailSettingsResponse(
        boolean enabled,
        String host,
        Integer port,
        String username,
        boolean passwordConfigured,
        String fromAddress,
        boolean smtpAuth,
        boolean startTlsEnable,
        boolean startTlsRequired,
        boolean sslEnable,
        boolean debug,
        Integer connectionTimeoutMs,
        Integer timeoutMs,
        Integer writeTimeoutMs,
        String confirmationBaseUrl,
        Instant updatedAt,
        boolean usingDatabaseSettings
) {
    public static EmailSettingsResponse from(ResolvedEmailSettings settings) {
        return new EmailSettingsResponse(
                settings.enabled(),
                settings.host(),
                settings.port(),
                settings.username(),
                settings.password() != null && !settings.password().isBlank(),
                settings.fromAddress(),
                settings.smtpAuth(),
                settings.startTlsEnable(),
                settings.startTlsRequired(),
                settings.sslEnable(),
                settings.debug(),
                settings.connectionTimeoutMs(),
                settings.timeoutMs(),
                settings.writeTimeoutMs(),
                settings.confirmationBaseUrl(),
                settings.updatedAt(),
                settings.usingDatabaseSettings()
        );
    }
}
