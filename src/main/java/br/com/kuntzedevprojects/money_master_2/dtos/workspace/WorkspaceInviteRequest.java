package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WorkspaceInviteRequest(
        @NotBlank(message = "O e-mail do convidado e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        String email,
        WorkspaceMemberRole role
) {
}
