package br.com.kuntzedevprojects.money_master_2.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRefreshRequest(@NotBlank String refreshToken) {
}
