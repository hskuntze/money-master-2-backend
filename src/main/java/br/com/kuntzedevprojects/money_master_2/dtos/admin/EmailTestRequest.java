package br.com.kuntzedevprojects.money_master_2.dtos.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTestRequest(
        @NotBlank @Email @Size(max = 180) String to
) {
}
