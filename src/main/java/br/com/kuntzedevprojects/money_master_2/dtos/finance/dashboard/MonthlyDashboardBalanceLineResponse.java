package br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard;

import java.math.BigDecimal;

public record MonthlyDashboardBalanceLineResponse(
        String key,
        String label,
        BigDecimal amount,
        String effect,
        String explanation
) {
}
