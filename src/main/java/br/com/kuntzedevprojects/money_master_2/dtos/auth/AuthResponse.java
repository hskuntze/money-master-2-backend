package br.com.kuntzedevprojects.money_master_2.dtos.auth;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.dtos.user.UserResponse;

public record AuthResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UserResponse user
) {
}
