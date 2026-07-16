package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.config.properties.SavingsJarYieldProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarMovementRequest;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarMovementRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarRepository;

class SavingsJarServiceTest {

    private final SavingsJarRepository savingsJarRepository = mock(SavingsJarRepository.class);
    private final SavingsJarMovementRepository movementRepository = mock(SavingsJarMovementRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final SavingsJarYieldService yieldService = mock(SavingsJarYieldService.class);
    private final SavingsJarYieldProperties yieldProperties = new SavingsJarYieldProperties();

    private final SavingsJarService service = new SavingsJarService(
            savingsJarRepository,
            movementRepository,
            currentUserService,
            accountService,
            financialPeriodService,
            yieldService,
            yieldProperties
    );

    @Test
    void deleteShouldArchiveJarWithMovementHistory() {
        SavingsJar jar = jar(true);
        jar.setYieldEnabled(true);
        when(savingsJarRepository.findByIdAndOwnerEmailWithAccount(jar.getId(), jar.getOwner().getEmail()))
                .thenReturn(Optional.of(jar));
        when(movementRepository.existsBySavingsJarId(jar.getId())).thenReturn(true);

        boolean deleted = service.delete(jar.getOwner().getEmail(), jar.getId());

        assertThat(deleted).isFalse();
        assertThat(jar.isActive()).isFalse();
        assertThat(jar.isYieldEnabled()).isFalse();
        verify(savingsJarRepository, never()).delete(jar);
        verify(movementRepository, never()).deleteBySavingsJarId(jar.getId());
    }

    @Test
    void deleteShouldHardDeleteJarWithoutMovementHistory() {
        SavingsJar jar = jar(true);
        when(savingsJarRepository.findByIdAndOwnerEmailWithAccount(jar.getId(), jar.getOwner().getEmail()))
                .thenReturn(Optional.of(jar));
        when(movementRepository.existsBySavingsJarId(jar.getId())).thenReturn(false);

        boolean deleted = service.delete(jar.getOwner().getEmail(), jar.getId());

        assertThat(deleted).isTrue();
        verify(savingsJarRepository).delete(jar);
        verify(movementRepository, never()).deleteBySavingsJarId(jar.getId());
    }

    @Test
    void depositShouldRejectArchivedJar() {
        SavingsJar jar = jar(false);
        when(savingsJarRepository.findByIdAndOwnerEmailWithAccount(jar.getId(), jar.getOwner().getEmail()))
                .thenReturn(Optional.of(jar));

        SavingsJarMovementRequest request = new SavingsJarMovementRequest(
                new BigDecimal("100.00"),
                LocalDate.of(2026, 7, 16),
                "Aporte",
                null,
                null
        );

        assertThatThrownBy(() -> service.deposit(jar.getOwner().getEmail(), jar.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("arquivado");
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
        jar.setTargetAmount(BigDecimal.ZERO);
        return jar;
    }
}
