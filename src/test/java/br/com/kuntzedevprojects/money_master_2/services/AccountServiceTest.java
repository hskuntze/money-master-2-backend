package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.config.properties.FinanceAiProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.repositories.AccountRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarMovementRepository;

class AccountServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
    private final SavingsJarMovementRepository savingsJarMovementRepository = mock(SavingsJarMovementRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final FinanceAiProperties financeAiProperties = new FinanceAiProperties();
    private final AccountService service = new AccountService(
            accountRepository,
            transactionRepository,
            savingsJarMovementRepository,
            currentUserService,
            financeAiProperties
    );

    @Test
    void balanceShouldApplyIncomingAndOutgoingTransfersWithoutChangingIncomeOrExpenseTotals() {
        Account account = account();
        String ownerEmail = account.getOwner().getEmail();
        when(accountRepository.findByIdAndOwnerEmailIgnoreCase(account.getId(), ownerEmail)).thenReturn(Optional.of(account));
        when(transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.INCOME))
                .thenReturn(new BigDecimal("50.00"));
        when(transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("20.00"));
        when(transactionRepository.sumOutgoingTransfers(ownerEmail, account.getId(), TransactionType.TRANSFER)).thenReturn(new BigDecimal("30.00"));
        when(transactionRepository.sumIncomingTransfers(ownerEmail, account.getId(), TransactionType.TRANSFER)).thenReturn(new BigDecimal("10.00"));
        when(savingsJarMovementRepository.calculateReservedAmountByLinkedAccount(ownerEmail, account.getId())).thenReturn(BigDecimal.ZERO);

        AccountBalanceResponse balance = service.balance(ownerEmail, account.getId());

        assertThat(balance.incomeTotal()).isEqualByComparingTo("50.00");
        assertThat(balance.expenseTotal()).isEqualByComparingTo("20.00");
        assertThat(balance.transferTotal()).isEqualByComparingTo("40.00");
        assertThat(balance.currentBalance()).isEqualByComparingTo("110.00");
        assertThat(balance.reservedInSavingsJars()).isEqualByComparingTo("0.00");
        assertThat(balance.availableBalance()).isEqualByComparingTo("110.00");
    }

    @Test
    void balanceShouldSeparateSavingsJarReservationFromAvailableBalance() {
        Account account = account();
        String ownerEmail = account.getOwner().getEmail();
        when(accountRepository.findByIdAndOwnerEmailIgnoreCase(account.getId(), ownerEmail)).thenReturn(Optional.of(account));
        when(transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.INCOME))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("150.00"));
        when(transactionRepository.sumOutgoingTransfers(ownerEmail, account.getId(), TransactionType.TRANSFER)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumIncomingTransfers(ownerEmail, account.getId(), TransactionType.TRANSFER)).thenReturn(BigDecimal.ZERO);
        when(savingsJarMovementRepository.calculateReservedAmountByLinkedAccount(ownerEmail, account.getId()))
                .thenReturn(new BigDecimal("300.00"));

        AccountBalanceResponse balance = service.balance(ownerEmail, account.getId());

        assertThat(balance.currentBalance()).isEqualByComparingTo("950.00");
        assertThat(balance.reservedInSavingsJars()).isEqualByComparingTo("300.00");
        assertThat(balance.availableBalance()).isEqualByComparingTo("650.00");
    }

    private Account account() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("User");
        owner.setEmail("user@example.com");

        Account account = new Account();
        account.setId(10L);
        account.setOwner(owner);
        account.setName("Conta corrente");
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("100.00"));
        account.setActive(true);
        return account;
    }
}
