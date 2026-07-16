package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import br.com.kuntzedevprojects.money_master_2.enums.FinancialWorkspaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceCreateRequest(
        @NotBlank(message = "O nome do espaco financeiro e obrigatorio.")
        @Size(max = 140, message = "O nome deve ter no maximo 140 caracteres.")
        String name,
        FinancialWorkspaceType type
) {
}
