package br.com.kuntzedevprojects.money_master_2.dtos.finance.debt;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.DebtAmortizationMethod;
import br.com.kuntzedevprojects.money_master_2.enums.DebtType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DebtCreateRequest(
        @NotBlank(message = "O nome da divida e obrigatorio.")
        @Size(max = 255, message = "O nome da divida deve ter no maximo 255 caracteres.")
        String name,

        DebtType type,
        DebtAmortizationMethod amortizationMethod,
        Long accountId,
        Long categoryId,

        @NotNull(message = "O saldo devedor inicial e obrigatorio.")
        @DecimalMin(value = "0.01", message = "O saldo devedor inicial deve ser maior que zero.")
        BigDecimal principalAmount,

        @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero.")
        BigDecimal installmentAmount,

        @DecimalMin(value = "0.00", message = "A taxa anual nao pode ser negativa.")
        BigDecimal annualInterestRate,

        @DecimalMin(value = "0.00", message = "O CET anual nao pode ser negativo.")
        BigDecimal annualCetRate,

        @DecimalMin(value = "0.00", message = "O encargo mensal nao pode ser negativo.")
        BigDecimal monthlyFeeAmount,

        @NotNull(message = "A quantidade de parcelas e obrigatoria.")
        Integer installmentCount,

        LocalDate startDate,

        @NotNull(message = "A data da primeira parcela e obrigatoria.")
        LocalDate firstDueDate,

        @Size(max = 2000, message = "As observacoes devem ter no maximo 2000 caracteres.")
        String notes
) {
}
