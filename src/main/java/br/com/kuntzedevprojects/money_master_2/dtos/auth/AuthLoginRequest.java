package br.com.kuntzedevprojects.money_master_2.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthLoginRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 80) String password
) {
}
