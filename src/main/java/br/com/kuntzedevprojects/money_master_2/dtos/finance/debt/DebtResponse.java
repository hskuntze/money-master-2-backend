package br.com.kuntzedevprojects.money_master_2.dtos.finance.debt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.entities.Debt;
import br.com.kuntzedevprojects.money_master_2.enums.DebtAmortizationMethod;
import br.com.kuntzedevprojects.money_master_2.enums.DebtInstallmentStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtType;

public record DebtResponse(
        Long id,
        String name,
        DebtType type,
        DebtStatus status,
        DebtAmortizationMethod amortizationMethod,
        BigDecimal principalAmount,
        BigDecimal installmentAmount,
        BigDecimal annualInterestRate,
        BigDecimal annualCetRate,
        BigDecimal monthlyFeeAmount,
        Integer installmentCount,
        LocalDate startDate,
        LocalDate firstDueDate,
        LocalDate lastDueDate,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        String notes,
        BigDecimal totalScheduledAmount,
        BigDecimal totalInterestAmount,
        BigDecimal totalFeeAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingPrincipalAmount,
        BigDecimal outstandingScheduledAmount,
        Integer paidInstallments,
        Integer overdueInstallments,
        Integer pendingInstallments,
        List<DebtInstallmentResponse> installments,
        Instant createdAt,
        Instant updatedAt,
        Instant canceledAt,
        Instant paidOffAt
) {
    public static DebtResponse from(Debt debt) {
        List<DebtInstallmentResponse> installmentResponses = debt.getInstallments() == null
                ? List.of()
                : debt.getInstallments().stream()
                        .sorted(Comparator.comparing(item -> item.getInstallmentNumber() == null ? 0 : item.getInstallmentNumber()))
                        .map(DebtInstallmentResponse::from)
                        .toList();
        BigDecimal totalScheduled = installmentResponses.stream()
                .map(DebtInstallmentResponse::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = installmentResponses.stream()
                .map(DebtInstallmentResponse::interestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFee = installmentResponses.stream()
                .map(DebtInstallmentResponse::feeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = installmentResponses.stream()
                .map(DebtInstallmentResponse::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstandingPrincipal = installmentResponses.stream()
                .filter(item -> item.status() != DebtInstallmentStatus.CANCELED)
                .map(DebtInstallmentResponse::outstandingPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstandingScheduled = installmentResponses.stream()
                .filter(item -> item.status() != DebtInstallmentStatus.CANCELED)
                .map(DebtInstallmentResponse::pendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int paidInstallments = (int) installmentResponses.stream()
                .filter(item -> item.status() == DebtInstallmentStatus.PAID || item.status() == DebtInstallmentStatus.PAID_IN_ADVANCE)
                .count();
        int overdueInstallments = (int) installmentResponses.stream()
                .filter(item -> item.status() == DebtInstallmentStatus.OVERDUE)
                .count();
        int pendingInstallments = (int) installmentResponses.stream()
                .filter(item -> item.status() == DebtInstallmentStatus.PENDING || item.status() == DebtInstallmentStatus.PARTIALLY_PAID)
                .count();
        DebtStatus status = debt.getStatus();
        if (status == DebtStatus.ACTIVE && !installmentResponses.isEmpty() && pendingInstallments == 0 && overdueInstallments == 0) {
            status = DebtStatus.PAID_OFF;
        }
        return new DebtResponse(
                debt.getId(),
                debt.getName(),
                debt.getType(),
                status,
                debt.getAmortizationMethod(),
                nullToZero(debt.getPrincipalAmount()),
                debt.getInstallmentAmount() == null ? null : nullToZero(debt.getInstallmentAmount()),
                debt.getAnnualInterestRate(),
                debt.getAnnualCetRate(),
                debt.getMonthlyFeeAmount() == null ? null : nullToZero(debt.getMonthlyFeeAmount()),
                debt.getInstallmentCount(),
                debt.getStartDate(),
                debt.getFirstDueDate(),
                debt.getLastDueDate(),
                debt.getAccount() == null ? null : debt.getAccount().getId(),
                debt.getAccount() == null ? null : debt.getAccount().getName(),
                debt.getCategory() == null ? null : debt.getCategory().getId(),
                debt.getCategory() == null ? null : debt.getCategory().getName(),
                debt.getNotes(),
                totalScheduled,
                totalInterest,
                totalFee,
                paid,
                outstandingPrincipal,
                outstandingScheduled,
                paidInstallments,
                overdueInstallments,
                pendingInstallments,
                installmentResponses,
                debt.getCreatedAt(),
                debt.getUpdatedAt(),
                debt.getCanceledAt(),
                debt.getPaidOffAt()
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }
}
