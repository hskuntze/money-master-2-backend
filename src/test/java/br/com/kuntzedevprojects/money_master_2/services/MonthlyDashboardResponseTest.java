package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardAlertResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardBalanceLineResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardBalanceOverviewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardBalanceViewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardResponse;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;

class MonthlyDashboardResponseTest {

    @Test
    void exposesSeparatedDashboardTotalsAndAlerts() {
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
        MonthlyDashboardAlertResponse alert = new MonthlyDashboardAlertResponse(
                "PROJECTED_BALANCE_NEGATIVE",
                "HIGH",
                "Saldo projetado negativo",
                "O saldo projetado do ciclo esta abaixo de zero.",
                null,
                null,
                null,
                new BigDecimal("-100.00")
        );
        MonthlyDashboardBalanceOverviewResponse balanceOverview = new MonthlyDashboardBalanceOverviewResponse(
                new MonthlyDashboardBalanceViewResponse(
                        "PLANNING",
                        "Planejamento",
                        "Combina receitas previstas, compromissos e baixas.",
                        new BigDecimal("5000.00"),
                        new BigDecimal("3400.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("1900.00"),
                        new BigDecimal("1200.00"),
                        List.of(new MonthlyDashboardBalanceLineResponse(
                                "CREDIT_CARD_INVOICES",
                                "Faturas do mes",
                                new BigDecimal("1200.00"),
                                "SUBTRACTS",
                                "Valor esperado ou confirmado das faturas vinculadas ao ciclo."
                        ))
                ),
                new MonthlyDashboardBalanceViewResponse(
                        "CASH",
                        "Caixa",
                        "Mostra o dinheiro atualmente registrado nas contas.",
                        new BigDecimal("4500.00"),
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("3200.00"),
                        List.of(new MonthlyDashboardBalanceLineResponse(
                                "CURRENT_ACCOUNT_BALANCE",
                                "Saldo atual em contas",
                                new BigDecimal("3200.00"),
                                "INFO",
                                "Soma dos saldos atuais das contas ativas acompanhadas."
                        ))
                ),
                new BigDecimal("-2000.00")
        );

        MonthlyDashboardResponse response = new MonthlyDashboardResponse(
                cycle,
                new BigDecimal("5000.00"),
                new BigDecimal("4500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1800.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("300.00"),
                new BigDecimal("900.00"),
                new BigDecimal("250.00"),
                new BigDecimal("900.00"),
                new BigDecimal("150.00"),
                new BigDecimal("400.00"),
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("3200.00"),
                new BigDecimal("1600.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("1200.00"),
                balanceOverview,
                List.of(alert)
        );

        assertThat(response.cycle().id()).isEqualTo(1L);
        assertThat(response.creditCardInvoicesPendingTotal()).isEqualByComparingTo("900.00");
        assertThat(response.savingsPlannedTotal()).isEqualByComparingTo("400.00");
        assertThat(response.investmentPlannedTotal()).isEqualByComparingTo("300.00");
        assertThat(response.balanceOverview().planning().availableAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.balanceOverview().cash().availableAmount()).isEqualByComparingTo("3200.00");
        assertThat(response.balanceOverview().planning().lines())
                .extracting(MonthlyDashboardBalanceLineResponse::key)
                .containsExactly("CREDIT_CARD_INVOICES");
        assertThat(response.alerts()).extracting(MonthlyDashboardAlertResponse::type)
                .containsExactly("PROJECTED_BALANCE_NEGATIVE");
    }
}
