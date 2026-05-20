package br.com.kuntzedevprojects.money_master_2.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
