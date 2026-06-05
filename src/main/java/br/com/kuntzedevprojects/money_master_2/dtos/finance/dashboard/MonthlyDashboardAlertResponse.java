package br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyDashboardAlertResponse(
        String type,
        String severity,
        String title,
        String message,
        String entityType,
        Long entityId,
        LocalDate dueDate,
        BigDecimal amount
) {
}
