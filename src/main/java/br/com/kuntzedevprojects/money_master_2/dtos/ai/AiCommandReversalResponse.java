package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import br.com.kuntzedevprojects.money_master_2.enums.AiCommandReversalStatus;

public record AiCommandReversalResponse(
        AiCommandReversalStatus status,
        String label,
        String hint,
        String referenceType,
        Long referenceId
) {
}
