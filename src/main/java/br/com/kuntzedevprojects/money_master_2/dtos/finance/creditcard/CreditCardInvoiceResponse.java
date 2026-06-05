package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceStatus;

public record CreditCardInvoiceResponse(
        Long id,
        Long creditCardId,
        String creditCardName,
        Long cycleId,
        String cycleName,
        Long monthlyPayableId,
        Integer referenceMonth,
        Integer referenceYear,
        LocalDate openingDate,
        LocalDate closingDate,
        LocalDate dueDate,
        CreditCardInvoiceStatus status,
        BigDecimal expectedAmount,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt,
        Instant paidAt
) {
    public static CreditCardInvoiceResponse from(CreditCardInvoice invoice) {
        BigDecimal finalAmount = nullToZero(invoice.getFinalAmount());
        BigDecimal paidAmount = nullToZero(invoice.getPaidAmount());
        return new CreditCardInvoiceResponse(
                invoice.getId(),
                invoice.getCreditCard().getId(),
                invoice.getCreditCard().getName(),
                invoice.getCycle().getId(),
                invoice.getCycle().getName(),
                invoice.getMonthlyPayable() == null ? null : invoice.getMonthlyPayable().getId(),
                invoice.getReferenceMonth(),
                invoice.getReferenceYear(),
                invoice.getOpeningDate(),
                invoice.getClosingDate(),
                invoice.getDueDate(),
                invoice.getStatus(),
                nullToZero(invoice.getExpectedAmount()),
                finalAmount,
                paidAmount,
                finalAmount.subtract(paidAmount).max(BigDecimal.ZERO),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                invoice.getClosedAt(),
                invoice.getPaidAt()
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
