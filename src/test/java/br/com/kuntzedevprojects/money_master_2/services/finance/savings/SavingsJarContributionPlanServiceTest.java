package br.com.kuntzedevprojects.money_master_2.services.finance.savings;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;

class SavingsJarContributionPlanServiceTest {

    private final SavingsJarService savingsJarService = mock(SavingsJarService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final SavingsJarContributionPlanService service = new SavingsJarContributionPlanService(
            savingsJarService,
            financialPeriodService
    );

    @Test
    void createShouldRejectArchivedJar() {
        SavingsJar jar = jar(false);
        when(savingsJarService.findOwnedJar(jar.getOwner().getEmail(), jar.getId())).thenReturn(jar);

        SavingsJarContributionPlanRequest request = new SavingsJarContributionPlanRequest(
                20L,
                new BigDecimal("150.00"),
                LocalDate.of(2026, 7, 20),
                false,
                null,
                null
        );

        assertThatThrownBy(() -> service.create(jar.getOwner().getEmail(), 20L, jar.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("arquivado");
        verifyNoInteractions(financialPeriodService);
    }

    private SavingsJar jar(boolean active) {
        User owner = new User();
        owner.setId(1L);
        owner.setName("User");
        owner.setEmail("user@example.com");

        SavingsJar jar = new SavingsJar();
        jar.setId(10L);
        jar.setOwner(owner);
        jar.setName("Reserva");
        jar.setActive(active);
        return jar;
    }
}
