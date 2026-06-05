package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationResponse;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipation;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipationItem;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentAnticipationStatus;

class InstallmentAnticipationResponseTest {

    @Test
    void exposesAuditAmountsAndSelectedInstallments() {
        InstallmentPurchase purchase = new InstallmentPurchase();
        purchase.setId(10L);
        purchase.setDescription("Sofa");

        FinancialPeriod cycle = new FinancialPeriod();
        cycle.setId(20L);
        cycle.setName("Junho");

        InstallmentPurchaseEntry entry = new InstallmentPurchaseEntry();
        entry.setId(30L);

        InstallmentAnticipationItem item = new InstallmentAnticipationItem();
        item.setInstallment(entry);
        item.setOriginalDueDate(LocalDate.of(2026, 10, 10));
        item.setOriginalAmount(new BigDecimal("200.00"));
        item.setAnticipatedAmount(new BigDecimal("180.00"));

        InstallmentAnticipation anticipation = new InstallmentAnticipation();
        anticipation.setId(40L);
        anticipation.setPurchase(purchase);
        anticipation.setCycle(cycle);
        anticipation.setAnticipationDate(LocalDate.of(2026, 6, 5));
        anticipation.setOriginalAmount(new BigDecimal("200.00"));
        anticipation.setAnticipatedAmount(new BigDecimal("180.00"));
        anticipation.setDiscountAmount(new BigDecimal("20.00"));
        anticipation.setStatus(InstallmentAnticipationStatus.ACTIVE);
        anticipation.addItem(item);

        InstallmentAnticipationResponse response = InstallmentAnticipationResponse.from(anticipation);

        assertThat(response.purchaseId()).isEqualTo(10L);
        assertThat(response.installmentIds()).containsExactly(30L);
        assertThat(response.originalAmount()).isEqualByComparingTo("200.00");
        assertThat(response.anticipatedAmount()).isEqualByComparingTo("180.00");
        assertThat(response.discountAmount()).isEqualByComparingTo("20.00");
    }
}
