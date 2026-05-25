package br.com.kuntzedevprojects.money_master_2.dtos.reference;

import br.com.kuntzedevprojects.money_master_2.enums.FinancialReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FinancialReferenceRequest(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 180)
        String title,

        @NotNull(message = "O tipo da referência é obrigatório.")
        FinancialReferenceType type,

        @Size(max = 1000)
        String url,

        @Size(max = 2000)
        String description,

        @Size(max = 180)
        String source,

        Boolean active
) {
}
