package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashFlowResponse(
        LocalDate date,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal netResult
) {
}
