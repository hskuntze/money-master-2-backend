package br.com.kuntzedevprojects.money_master_2.dtos.finance.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.Payment;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentSource;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus;

public record PaymentResponse(
        Long id,
        Long cycleId,
        String cycleName,
        Long payableId,
        Long incomePlanId,
        Long transactionId,
        Long accountId,
        String accountName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        PaymentSource source,
        PaymentStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Instant reversedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCycle() == null ? null : payment.getCycle().getId(),
                payment.getCycle() == null ? null : payment.getCycle().getName(),
                payment.getPayable() == null ? null : payment.getPayable().getId(),
                payment.getIncomePlan() == null ? null : payment.getIncomePlan().getId(),
                payment.getTransaction() == null ? null : payment.getTransaction().getId(),
                payment.getAccount() == null ? null : payment.getAccount().getId(),
                payment.getAccount() == null ? null : payment.getAccount().getName(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMethod(),
                payment.getSource(),
                payment.getStatus(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getReversedAt()
        );
    }
}
