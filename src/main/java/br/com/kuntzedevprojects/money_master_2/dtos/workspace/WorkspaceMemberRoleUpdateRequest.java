package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberRole;
import jakarta.validation.constraints.NotNull;

public record WorkspaceMemberRoleUpdateRequest(
        @NotNull(message = "O papel do membro e obrigatorio.")
        WorkspaceMemberRole role
) {
}
