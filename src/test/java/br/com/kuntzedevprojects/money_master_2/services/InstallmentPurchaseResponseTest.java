package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentMode;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus;

class InstallmentPurchaseResponseTest {

    @Test
    void exposesCreditCardInstallmentsAsInvoiceItems() {
        CreditCard card = new CreditCard();
        card.setId(12L);
        card.setName("Nubank");

        CreditCardInvoice invoice = new CreditCardInvoice();
        invoice.setId(34L);
        invoice.setCreditCard(card);
        invoice.setReferenceMonth(6);
        invoice.setReferenceYear(2026);

        CreditCardInvoiceItem invoiceItem = new CreditCardInvoiceItem();
        invoiceItem.setId(56L);
        invoiceItem.setInvoice(invoice);

        InstallmentPurchase purchase = new InstallmentPurchase();
        purchase.setId(78L);
        purchase.setDescription("Notebook");
        purchase.setTotalAmount(new BigDecimal("600.00"));
        purchase.setInstallmentCount(3);
        purchase.setInstallmentAmount(new BigDecimal("200.00"));
        purchase.setPurchaseDate(LocalDate.of(2026, 6, 1));
        purchase.setFirstDueDate(LocalDate.of(2026, 6, 15));
        purchase.setLastDueDate(LocalDate.of(2026, 8, 15));
        purchase.setStatus(InstallmentPurchaseStatus.ACTIVE);
        purchase.setPaymentMode(InstallmentPaymentMode.CREDIT_CARD);
        purchase.setCreditCard(card);
        purchase.setFirstInvoice(invoice);

        InstallmentPurchaseEntry entry = new InstallmentPurchaseEntry();
        entry.setPurchase(purchase);
        entry.setInstallmentNumber(1);
        entry.setDueDate(LocalDate.of(2026, 6, 15));
        entry.setAmount(new BigDecimal("200.00"));
        entry.setStatus(InstallmentEntryStatus.IN_INVOICE);
        entry.setInvoiceItem(invoiceItem);
        purchase.addEntry(entry);

        InstallmentPurchaseResponse response = InstallmentPurchaseResponse.from(purchase);

        assertThat(response.paymentMode()).isEqualTo(InstallmentPaymentMode.CREDIT_CARD);
        assertThat(response.creditCardId()).isEqualTo(12L);
        assertThat(response.firstInvoiceId()).isEqualTo(34L);
        assertThat(response.postedInstallments()).isEqualTo(1);
        assertThat(response.entries().get(0).invoiceItemId()).isEqualTo(56L);
        assertThat(response.entries().get(0).invoiceId()).isEqualTo(34L);
    }
}
