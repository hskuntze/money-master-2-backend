package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.config.properties.FinanceAiProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.AccountRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final FinanceAiProperties financeAiProperties;

    public AccountService(
            AccountRepository accountRepository,
            FinancialTransactionRepository transactionRepository,
            CurrentUserService currentUserService,
            FinanceAiProperties financeAiProperties
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.financeAiProperties = financeAiProperties;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(String ownerEmail) {
        return accountRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(ownerEmail)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String ownerEmail, Long id) {
        return AccountResponse.from(findOwnedAccount(ownerEmail, id));
    }

    @Transactional
    public AccountResponse create(String ownerEmail, AccountCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        String name = normalizeRequired(request.name(), "O nome da conta é obrigatório.");

        if (accountRepository.existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(ownerEmail, name)) {
            throw new BusinessException("Já existe uma conta com este nome.");
        }

        Account account = new Account();
        account.setOwner(owner);
        account.setName(name);
        account.setType(request.type());
        account.setInitialBalance(nullToZero(request.initialBalance()));
        account.setActive(request.active() == null || request.active());

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(String ownerEmail, Long id, AccountUpdateRequest request) {
        Account account = findOwnedAccount(ownerEmail, id);

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            accountRepository.findByOwnerEmailIgnoreCaseAndNameIgnoreCase(ownerEmail, name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BusinessException("Já existe outra conta com este nome.");
                    });
            account.setName(name);
        }
        if (request.type() != null) {
            account.setType(request.type());
        }
        if (request.initialBalance() != null) {
            account.setInitialBalance(request.initialBalance());
        }
        if (request.active() != null) {
            account.setActive(request.active());
        }

        return AccountResponse.from(account);
    }

    @Transactional
    public void deactivate(String ownerEmail, Long id) {
        Account account = findOwnedAccount(ownerEmail, id);
        account.setActive(false);
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceResponse> balances(String ownerEmail) {
        return accountRepository.findByOwnerEmailIgnoreCaseOrderByNameAsc(ownerEmail)
                .stream()
                .map(account -> balance(ownerEmail, account))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse balance(String ownerEmail, Long accountId) {
        return balance(ownerEmail, findOwnedAccount(ownerEmail, accountId));
    }

    @Transactional
    public Account resolveForAi(String ownerEmail, Long accountId, String accountName) {
        if (accountId != null) {
            Account account = findOwnedAccount(ownerEmail, accountId);
            if (!account.isActive()) {
                throw new BusinessException("A conta informada está inativa.");
            }
            return account;
        }

        if (accountName != null && !accountName.isBlank()) {
            return accountRepository.findByOwnerEmailIgnoreCaseAndNameIgnoreCase(ownerEmail, accountName.trim())
                    .filter(Account::isActive)
                    .orElseGet(() -> createDefaultAccount(ownerEmail, accountName.trim()));
        }

        List<Account> activeAccounts = accountRepository.findByOwnerEmailIgnoreCaseAndActiveTrueOrderByNameAsc(ownerEmail);
        if (activeAccounts.size() == 1) {
            return activeAccounts.get(0);
        }

        return accountRepository.findByOwnerEmailIgnoreCaseAndNameIgnoreCase(ownerEmail, financeAiProperties.getDefaultAccountName())
                .filter(Account::isActive)
                .orElseGet(() -> createDefaultAccount(ownerEmail, financeAiProperties.getDefaultAccountName()));
    }

    @Transactional(readOnly = true)
    public Account findOwnedAccount(String ownerEmail, Long id) {
        return accountRepository.findByIdAndOwnerEmailIgnoreCase(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
    }

    private AccountBalanceResponse balance(String ownerEmail, Account account) {
        BigDecimal incomeTotal = transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.INCOME);
        BigDecimal expenseTotal = transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.EXPENSE);
        BigDecimal transferTotal = transactionRepository.sumAmount(ownerEmail, account.getId(), null, null, null, TransactionType.TRANSFER);
        BigDecimal currentBalance = account.getInitialBalance()
                .add(nullToZero(incomeTotal))
                .subtract(nullToZero(expenseTotal));

        return new AccountBalanceResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                nullToZero(incomeTotal),
                nullToZero(expenseTotal),
                nullToZero(transferTotal),
                currentBalance
        );
    }

    private Account createDefaultAccount(String ownerEmail, String accountName) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        Account account = new Account();
        account.setOwner(owner);
        account.setName(accountName);
        account.setType(resolveDefaultAccountType());
        account.setInitialBalance(BigDecimal.ZERO);
        account.setActive(true);
        return accountRepository.save(account);
    }

    private AccountType resolveDefaultAccountType() {
        try {
            return AccountType.valueOf(financeAiProperties.getDefaultAccountType());
        } catch (Exception ex) {
            return AccountType.CHECKING;
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
