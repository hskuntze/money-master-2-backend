package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import jakarta.validation.constraints.Size;

public record WorkspaceUpdateRequest(
        @Size(max = 140, message = "O nome deve ter no maximo 140 caracteres.")
        String name
) {
}
