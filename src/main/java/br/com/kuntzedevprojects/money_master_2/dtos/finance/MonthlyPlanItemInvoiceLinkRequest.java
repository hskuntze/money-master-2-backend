package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemInvoiceContributionMode;

public record MonthlyPlanItemInvoiceLinkRequest(
        MonthlyPlanItemInvoiceContributionMode invoiceContributionMode
) {
}
