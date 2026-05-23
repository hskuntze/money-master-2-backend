package br.com.kuntzedevprojects.money_master_2.dtos.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailSettingsUpdateRequest(
        @NotNull Boolean enabled,
        @NotBlank @Size(max = 180) String host,
        @NotNull @Min(1) @Max(65535) Integer port,
        @Size(max = 180) String username,
        @Size(max = 500) String password,
        @NotBlank @Email @Size(max = 180) String fromAddress,
        @NotNull Boolean smtpAuth,
        @NotNull Boolean startTlsEnable,
        @NotNull Boolean startTlsRequired,
        @NotNull Boolean sslEnable,
        @NotNull Boolean debug,
        @Min(1000) Integer connectionTimeoutMs,
        @Min(1000) Integer timeoutMs,
        @Min(1000) Integer writeTimeoutMs,
        @NotBlank @Size(max = 500) String confirmationBaseUrl
) {
}
