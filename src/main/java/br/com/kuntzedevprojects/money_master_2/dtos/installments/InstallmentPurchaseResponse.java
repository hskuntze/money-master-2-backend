package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus;

public record InstallmentPurchaseResponse(
        Long id,
        String description,
        BigDecimal totalAmount,
        Integer installmentCount,
        BigDecimal installmentAmount,
        LocalDate purchaseDate,
        LocalDate firstDueDate,
        LocalDate lastDueDate,
        InstallmentPurchaseStatus status,
        Long categoryId,
        String categoryName,
        String notes,
        Integer postedInstallments,
        Integer paidInstallments,
        Integer pendingInstallments,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        List<InstallmentPurchaseEntryResponse> entries,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstallmentPurchaseResponse from(InstallmentPurchase purchase) {
        List<InstallmentPurchaseEntryResponse> entryResponses = purchase.getEntries() == null
                ? List.of()
                : purchase.getEntries().stream()
                .sorted(java.util.Comparator.comparing(entry -> entry.getInstallmentNumber() == null ? 0 : entry.getInstallmentNumber()))
                .map(InstallmentPurchaseEntryResponse::from)
                .toList();
        int posted = (int) entryResponses.stream().filter(entry -> entry.monthlyPlanItemId() != null).count();
        int paid = (int) purchase.getEntries().stream().filter(entry -> entry.getStatus() == InstallmentEntryStatus.PAID).count();
        int pending = (int) purchase.getEntries().stream()
                .filter(entry -> entry.getStatus() != InstallmentEntryStatus.PAID)
                .filter(entry -> entry.getStatus() != InstallmentEntryStatus.CANCELED)
                .count();
        BigDecimal paidAmount = purchase.getEntries().stream()
                .filter(entry -> entry.getStatus() == InstallmentEntryStatus.PAID)
                .map(entry -> entry.getAmount() == null ? BigDecimal.ZERO : entry.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = purchase.getTotalAmount() == null ? BigDecimal.ZERO : purchase.getTotalAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        return new InstallmentPurchaseResponse(
                purchase.getId(),
                purchase.getDescription(),
                purchase.getTotalAmount(),
                purchase.getInstallmentCount(),
                purchase.getInstallmentAmount(),
                purchase.getPurchaseDate(),
                purchase.getFirstDueDate(),
                purchase.getLastDueDate(),
                purchase.getStatus(),
                purchase.getCategory() == null ? null : purchase.getCategory().getId(),
                purchase.getCategory() == null ? null : purchase.getCategory().getName(),
                purchase.getNotes(),
                posted,
                paid,
                pending,
                paidAmount,
                remaining,
                entryResponses,
                purchase.getCreatedAt(),
                purchase.getUpdatedAt()
        );
    }
}
