package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentMode;

public record InstallmentAnticipationPreviewResponse(
        Long purchaseId,
        String purchaseDescription,
        InstallmentPaymentMode paymentMode,
        List<InstallmentPurchaseEntryResponse> selectedInstallments,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal anticipatedAmount,
        Long impactedCycleId,
        String impactedCycleName,
        Long targetInvoiceId,
        String targetInvoiceDescription,
        List<Long> futureInvoiceIds,
        List<Long> futurePayableIds,
        String projectedImpact,
        String previewToken
) {
}
