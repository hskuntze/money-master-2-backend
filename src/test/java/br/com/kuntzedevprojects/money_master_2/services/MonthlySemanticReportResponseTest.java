package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.report.MonthlySemanticReportResponse;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;

class MonthlySemanticReportResponseTest {

    @Test
    void separatesPlanningRealizedCardsInstallmentsSavingsAndInvestments() {
        MonthlyCycleResponse cycle = new MonthlyCycleResponse(
                1L,
                "Junho",
                6,
                2026,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                1,
                FinancialPeriodStatus.OPEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null
        );

        MonthlySemanticReportResponse response = new MonthlySemanticReportResponse(
                cycle,
                new MonthlySemanticReportResponse.PlanningSection(
                        new BigDecimal("5000.00"),
                        new BigDecimal("1600.00"),
                        new BigDecimal("1200.00"),
                        new BigDecimal("400.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("1800.00")
                ),
                new MonthlySemanticReportResponse.RealizedSection(
                        new BigDecimal("4500.00"),
                        new BigDecimal("900.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("3100.00")
                ),
                new MonthlySemanticReportResponse.CreditCardSection(
                        new BigDecimal("1200.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("900.00")
                ),
                new MonthlySemanticReportResponse.InstallmentSection(
                        new BigDecimal("250.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("150.00")
                ),
                new MonthlySemanticReportResponse.SavingsJarSection(
                        new BigDecimal("3000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("400.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("50.00"),
                        new BigDecimal("30.00")
                ),
                new MonthlySemanticReportResponse.InvestmentSection(
                        new BigDecimal("5000.00"),
                        new BigDecimal("4500.00"),
                        new BigDecimal("600.00"),
                        new BigDecimal("1100.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("100.00"),
                        2
                ),
                new BigDecimal("7000.00"),
                new BigDecimal("1800.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        assertThat(response.planning().plannedCreditCardInvoicesTotal()).isEqualByComparingTo("1200.00");
        assertThat(response.realized().paidCreditCardInvoicesTotal()).isEqualByComparingTo("300.00");
        assertThat(response.installments().futureTotal()).isEqualByComparingTo("1000.00");
        assertThat(response.savingsJars().monthlyPlannedContribution()).isEqualByComparingTo("400.00");
        assertThat(response.planning().plannedInvestmentTotal()).isEqualByComparingTo("250.00");
        assertThat(response.realized().actualInvestmentTotal()).isEqualByComparingTo("100.00");
        assertThat(response.investments().totalAmount()).isEqualByComparingTo("5000.00");
    }
}
