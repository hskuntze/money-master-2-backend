package br.com.kuntzedevprojects.money_master_2.dtos.user;

import java.util.Set;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 140) String name,
        Boolean enabled,
        Boolean accountNonLocked,
        Set<String> roles
) {
}
