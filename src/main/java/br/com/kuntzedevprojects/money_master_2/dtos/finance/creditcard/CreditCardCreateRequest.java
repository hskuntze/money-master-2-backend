package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreditCardCreateRequest(
        Long accountId,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 80) String brand,
        @PositiveOrZero BigDecimal limitAmount,
        @NotNull @Min(1) @Max(31) Integer closingDay,
        @NotNull @Min(1) @Max(31) Integer dueDay,
        Boolean active
) {
}
