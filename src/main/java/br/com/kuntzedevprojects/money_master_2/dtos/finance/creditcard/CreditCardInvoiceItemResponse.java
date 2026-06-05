package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;

public record CreditCardInvoiceItemResponse(
        Long id,
        Long invoiceId,
        Long categoryId,
        String categoryName,
        String description,
        BigDecimal amount,
        LocalDate purchaseDate,
        LocalDate competenceDate,
        CreditCardInvoiceItemSourceType sourceType,
        Long sourceId,
        Integer installmentNumber,
        Long transactionId,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static CreditCardInvoiceItemResponse from(CreditCardInvoiceItem item) {
        return new CreditCardInvoiceItemResponse(
                item.getId(),
                item.getInvoice().getId(),
                item.getCategory() == null ? null : item.getCategory().getId(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getDescription(),
                item.getAmount(),
                item.getPurchaseDate(),
                item.getCompetenceDate(),
                item.getSourceType(),
                item.getSourceId(),
                item.getInstallmentNumber(),
                item.getTransactionId(),
                item.getNotes(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
