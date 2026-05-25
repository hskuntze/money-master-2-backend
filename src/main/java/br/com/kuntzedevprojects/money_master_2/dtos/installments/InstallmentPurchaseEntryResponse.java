package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;

public record InstallmentPurchaseEntryResponse(
        Long id,
        Long purchaseId,
        Long financialPeriodId,
        String financialPeriodName,
        Long monthlyPlanItemId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        InstallmentEntryStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstallmentPurchaseEntryResponse from(InstallmentPurchaseEntry entry) {
        return new InstallmentPurchaseEntryResponse(
                entry.getId(),
                entry.getPurchase() == null ? null : entry.getPurchase().getId(),
                entry.getFinancialPeriod() == null ? null : entry.getFinancialPeriod().getId(),
                entry.getFinancialPeriod() == null ? null : entry.getFinancialPeriod().getName(),
                entry.getMonthlyPlanItem() == null ? null : entry.getMonthlyPlanItem().getId(),
                entry.getInstallmentNumber(),
                entry.getDueDate(),
                entry.getAmount(),
                entry.getStatus(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
