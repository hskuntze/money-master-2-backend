package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreditCardUpdateRequest(
        Long accountId,
        @Size(max = 120) String name,
        @Size(max = 80) String brand,
        @PositiveOrZero BigDecimal limitAmount,
        @Min(1) @Max(31) Integer closingDay,
        @Min(1) @Max(31) Integer dueDay,
        Boolean active
) {
}
