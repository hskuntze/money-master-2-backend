package br.com.kuntzedevprojects.money_master_2.services.finance.planning;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;
import br.com.kuntzedevprojects.money_master_2.services.finance.income.MonthlyIncomePlanService;
import br.com.kuntzedevprojects.money_master_2.services.finance.payable.MonthlyPayableService;

@Service
public class MonthlyPlanningService {

    private final MonthlyIncomePlanService incomePlanService;
    private final MonthlyPayableService payableService;

    public MonthlyPlanningService(MonthlyIncomePlanService incomePlanService, MonthlyPayableService payableService) {
        this.incomePlanService = incomePlanService;
        this.payableService = payableService;
    }

    @Transactional(readOnly = true)
    public MonthlyPlanningResponse getPlanning(String ownerEmail, Long cycleId) {
        return new MonthlyPlanningResponse(
                incomePlanService.list(ownerEmail, cycleId),
                payableService.list(ownerEmail, cycleId)
        );
    }

    public record MonthlyPlanningResponse(
            List<MonthlyIncomePlanResponse> incomePlans,
            List<MonthlyPayableResponse> payables
    ) {
    }
}
