package br.com.kuntzedevprojects.money_master_2.dtos.finance.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        Long transactionId,
        Long accountId,
        Long categoryId,
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        PaymentSource source,
        Boolean createTransaction,
        @Size(max = 2000, message = "As observacoes devem ter no maximo 2000 caracteres.")
        String notes
) {
}
