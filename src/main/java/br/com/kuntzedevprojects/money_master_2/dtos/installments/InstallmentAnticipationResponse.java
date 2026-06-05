package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipation;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipationItem;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentAnticipationStatus;

public record InstallmentAnticipationResponse(
        Long id,
        Long purchaseId,
        String purchaseDescription,
        Long creditCardId,
        String creditCardName,
        Long targetInvoiceId,
        Long cycleId,
        String cycleName,
        LocalDate anticipationDate,
        BigDecimal originalAmount,
        BigDecimal anticipatedAmount,
        BigDecimal discountAmount,
        InstallmentAnticipationStatus status,
        List<Long> installmentIds,
        String notes,
        Instant createdAt
) {
    public static InstallmentAnticipationResponse from(InstallmentAnticipation anticipation) {
        return new InstallmentAnticipationResponse(
                anticipation.getId(),
                anticipation.getPurchase().getId(),
                anticipation.getPurchase().getDescription(),
                anticipation.getCreditCard() == null ? null : anticipation.getCreditCard().getId(),
                anticipation.getCreditCard() == null ? null : anticipation.getCreditCard().getName(),
                anticipation.getTargetInvoice() == null ? null : anticipation.getTargetInvoice().getId(),
                anticipation.getCycle().getId(),
                anticipation.getCycle().getName(),
                anticipation.getAnticipationDate(),
                anticipation.getOriginalAmount(),
                anticipation.getAnticipatedAmount(),
                anticipation.getDiscountAmount(),
                anticipation.getStatus(),
                anticipation.getItems().stream().map(InstallmentAnticipationItem::getInstallment).map(entry -> entry.getId()).toList(),
                anticipation.getNotes(),
                anticipation.getCreatedAt()
        );
    }
}
