package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionCreateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;

class FinancialTransactionServiceTest {

    private final FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
    private final MonthlyPlanItemRepository planItemRepository = mock(MonthlyPlanItemRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final FinancialTransactionService service = new FinancialTransactionService(
            transactionRepository,
            planItemRepository,
            currentUserService,
            accountService,
            categoryService,
            financialPeriodService
    );

    @Test
    void createTransferShouldPersistSourceAndDestinationWithoutMonthlyPlanLink() {
        User owner = user();
        Account source = account(1L, "Conta corrente", owner);
        Account destination = account(2L, "Reserva", owner);
        FinancialPeriod period = period();
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(accountService.findOwnedAccount(owner.getEmail(), source.getId())).thenReturn(source);
        when(accountService.findOwnedAccount(owner.getEmail(), destination.getId())).thenReturn(destination);
        when(financialPeriodService.findOrCreateForDate(owner.getEmail(), LocalDate.of(2026, 7, 16))).thenReturn(period);
        when(transactionRepository.save(any(FinancialTransaction.class))).thenAnswer(invocation -> {
            FinancialTransaction transaction = invocation.getArgument(0);
            transaction.setId(99L);
            return transaction;
        });

        service.create(owner.getEmail(), transferRequest(source.getId(), destination.getId()));

        ArgumentCaptor<FinancialTransaction> captor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(captor.capture());
        FinancialTransaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(saved.getAccount()).isSameAs(source);
        assertThat(saved.getDestinationAccount()).isSameAs(destination);
        assertThat(saved.getMonthlyPlanItem()).isNull();
        verifyNoInteractions(planItemRepository);
        verify(financialPeriodService).registerPaymentForPlanItem(null, saved);
    }

    @Test
    void createTransferShouldRequireDestinationAccount() {
        User owner = user();
        Account source = account(1L, "Conta corrente", owner);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(accountService.findOwnedAccount(owner.getEmail(), source.getId())).thenReturn(source);

        assertThatThrownBy(() -> service.create(owner.getEmail(), transferRequest(source.getId(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("destino");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransferShouldRejectSameSourceAndDestination() {
        User owner = user();
        Account source = account(1L, "Conta corrente", owner);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(accountService.findOwnedAccount(owner.getEmail(), source.getId())).thenReturn(source);

        assertThatThrownBy(() -> service.create(owner.getEmail(), transferRequest(source.getId(), source.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("diferente");
        verify(transactionRepository, never()).save(any());
    }

    private FinancialTransactionCreateRequest transferRequest(Long sourceAccountId, Long destinationAccountId) {
        return new FinancialTransactionCreateRequest(
                sourceAccountId,
                destinationAccountId,
                null,
                null,
                null,
                TransactionType.TRANSFER,
                "Transferencia para reserva",
                new BigDecimal("250.00"),
                LocalDate.of(2026, 7, 16),
                TransactionSource.MANUAL,
                null
        );
    }

    private User user() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("User");
        owner.setEmail("user@example.com");
        return owner;
    }

    private Account account(Long id, String name, User owner) {
        Account account = new Account();
        account.setId(id);
        account.setOwner(owner);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(BigDecimal.ZERO);
        account.setActive(true);
        return account;
    }

    private FinancialPeriod period() {
        FinancialPeriod period = new FinancialPeriod();
        period.setId(10L);
        period.setName("Julho/2026");
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        return period;
    }
}
