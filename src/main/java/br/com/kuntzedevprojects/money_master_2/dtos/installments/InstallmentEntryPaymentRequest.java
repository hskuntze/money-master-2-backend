package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.time.LocalDate;

public record InstallmentEntryPaymentRequest(
        LocalDate paidOn
) {
}
