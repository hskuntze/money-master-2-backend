package br.com.kuntzedevprojects.money_master_2.dtos.finance.debt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.DebtInstallment;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.DebtInstallmentStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;

public record DebtInstallmentResponse(
        Long id,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        BigDecimal outstandingPrincipalAmount,
        BigDecimal balanceAfterPayment,
        DebtInstallmentStatus status,
        Long cycleId,
        String cycleName,
        Long monthlyPlanItemId,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static DebtInstallmentResponse from(DebtInstallment installment) {
        MonthlyPlanItem item = installment.getMonthlyPlanItem();
        BigDecimal total = nullToZero(installment.getTotalAmount());
        BigDecimal paid = item == null ? BigDecimal.ZERO : nullToZero(item.getActualAmount());
        BigDecimal pending = total.subtract(paid).max(BigDecimal.ZERO);
        BigDecimal principalPaid = paid.subtract(nullToZero(installment.getInterestAmount()))
                .subtract(nullToZero(installment.getFeeAmount()))
                .max(BigDecimal.ZERO);
        BigDecimal outstandingPrincipal = nullToZero(installment.getPrincipalAmount()).subtract(principalPaid).max(BigDecimal.ZERO);
        return new DebtInstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                nullToZero(installment.getPrincipalAmount()),
                nullToZero(installment.getInterestAmount()),
                nullToZero(installment.getFeeAmount()),
                total,
                paid,
                pending,
                outstandingPrincipal,
                nullToZero(installment.getBalanceAfterPayment()),
                resolveStatus(installment, item, pending),
                installment.getFinancialPeriod() == null ? null : installment.getFinancialPeriod().getId(),
                installment.getFinancialPeriod() == null ? null : installment.getFinancialPeriod().getName(),
                item == null ? null : item.getId(),
                installment.getNotes(),
                installment.getCreatedAt(),
                installment.getUpdatedAt()
        );
    }

    private static DebtInstallmentStatus resolveStatus(DebtInstallment installment, MonthlyPlanItem item, BigDecimal pending) {
        if (installment.getStatus() == DebtInstallmentStatus.CANCELED) {
            return DebtInstallmentStatus.CANCELED;
        }
        if (item == null) {
            return installment.getStatus();
        }
        if (item.getStatus() == MonthlyPlanItemStatus.CANCELED) {
            return DebtInstallmentStatus.CANCELED;
        }
        if (pending.signum() == 0 || item.getStatus() == MonthlyPlanItemStatus.PAID) {
            return item.getPaidOn() != null && item.getPaidOn().isBefore(installment.getDueDate())
                    ? DebtInstallmentStatus.PAID_IN_ADVANCE
                    : DebtInstallmentStatus.PAID;
        }
        if (item.getStatus() == MonthlyPlanItemStatus.PARTIALLY_PAID || nullToZero(item.getActualAmount()).signum() > 0) {
            return DebtInstallmentStatus.PARTIALLY_PAID;
        }
        if (installment.getDueDate() != null && installment.getDueDate().isBefore(LocalDate.now())) {
            return DebtInstallmentStatus.OVERDUE;
        }
        return DebtInstallmentStatus.PENDING;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }
}
