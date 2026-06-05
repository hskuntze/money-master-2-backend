package br.com.kuntzedevprojects.money_master_2.services.finance.cycle;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class MonthlyCycleQueryService {

    private final FinancialPeriodService financialPeriodService;

    public MonthlyCycleQueryService(FinancialPeriodService financialPeriodService) {
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<MonthlyCycleResponse> list(String ownerEmail) {
        return financialPeriodService.list(ownerEmail)
                .stream()
                .map(MonthlyCycleResponse::from)
                .toList();
    }

    @Transactional
    public MonthlyCycleResponse current(String ownerEmail) {
        return MonthlyCycleResponse.from(financialPeriodService.current(ownerEmail));
    }

    @Transactional(readOnly = true)
    public MonthlyCycleResponse get(String ownerEmail, Long id) {
        return MonthlyCycleResponse.from(financialPeriodService.get(ownerEmail, id));
    }

    @Transactional(readOnly = true)
    public MonthlyCycleSummaryResponse summary(String ownerEmail, Long id) {
        return MonthlyCycleSummaryResponse.from(financialPeriodService.summary(ownerEmail, id));
    }
}
