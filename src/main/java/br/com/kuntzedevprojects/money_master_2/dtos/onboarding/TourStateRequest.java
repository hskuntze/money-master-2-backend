package br.com.kuntzedevprojects.money_master_2.dtos.onboarding;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TourStateRequest(
        @Pattern(regexp = "COMPLETE|SKIP|RESET|PROGRESS", message = "A ação do tour deve ser COMPLETE, SKIP, RESET ou PROGRESS.")
        String action,

        @Size(max = 120)
        String lastStepKey
) {
}
