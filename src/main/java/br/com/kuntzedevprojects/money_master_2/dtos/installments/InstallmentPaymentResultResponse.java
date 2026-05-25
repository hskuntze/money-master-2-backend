package br.com.kuntzedevprojects.money_master_2.dtos.installments;

public record InstallmentPaymentResultResponse(
        String message,
        InstallmentPurchaseResponse purchase
) {
}
