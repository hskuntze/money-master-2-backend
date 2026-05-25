package br.com.kuntzedevprojects.money_master_2.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSelfUpdateRequest(
        @NotBlank @Size(max = 140) String name
) {
}
