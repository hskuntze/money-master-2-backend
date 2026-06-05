package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InstallmentAnticipationRequest(
        @NotEmpty List<Long> installmentIds,
        LocalDate anticipationDate,
        Long targetInvoiceId,
        Long accountId,
        @PositiveOrZero BigDecimal anticipatedAmount,
        @PositiveOrZero BigDecimal discountAmount,
        @Size(max = 2000) String notes,
        String confirmPreviewToken
) {
}
