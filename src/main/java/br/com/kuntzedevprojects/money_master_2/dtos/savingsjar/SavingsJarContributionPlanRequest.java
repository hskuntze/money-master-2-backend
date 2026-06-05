package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavingsJarContributionPlanRequest(
        @NotNull Long cycleId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate dueDate,
        Boolean recurring,
        LocalDate recurrenceEndDate,
        @Size(max = 2000) String notes
) {
}
