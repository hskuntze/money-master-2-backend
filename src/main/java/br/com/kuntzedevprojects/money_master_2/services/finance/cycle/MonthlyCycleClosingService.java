package br.com.kuntzedevprojects.money_master_2.services.finance.cycle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class MonthlyCycleClosingService {

    private final FinancialPeriodService financialPeriodService;

    public MonthlyCycleClosingService(FinancialPeriodService financialPeriodService) {
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional
    public MonthlyCycleResponse close(String ownerEmail, Long id) {
        return MonthlyCycleResponse.from(financialPeriodService.closePeriodById(ownerEmail, id));
    }

    @Transactional
    public MonthlyCycleResponse reopen(String ownerEmail, Long id) {
        return MonthlyCycleResponse.from(financialPeriodService.reopenPeriod(ownerEmail, id));
    }
}
