package br.com.kuntzedevprojects.money_master_2.dtos.auth;

import jakarta.validation.constraints.Size;

public record AuthLogoutRequest(
        @Size(max = 512) String refreshToken
) {
}
