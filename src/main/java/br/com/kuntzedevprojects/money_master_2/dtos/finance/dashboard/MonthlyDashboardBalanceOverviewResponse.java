package br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard;

import java.math.BigDecimal;

public record MonthlyDashboardBalanceOverviewResponse(
        MonthlyDashboardBalanceViewResponse planning,
        MonthlyDashboardBalanceViewResponse cash,
        BigDecimal projectedVsCashDifferenceAmount
) {
}
