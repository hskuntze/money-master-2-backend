package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;

@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;

    public CreditCardService(
            CreditCardRepository creditCardRepository,
            CurrentUserService currentUserService,
            AccountService accountService
    ) {
        this.creditCardRepository = creditCardRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> list(String ownerEmail) {
        return creditCardRepository.findByOwnerEmail(ownerEmail).stream().map(CreditCardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CreditCardResponse get(String ownerEmail, Long id) {
        return CreditCardResponse.from(findOwnedCard(ownerEmail, id));
    }

    @Transactional
    public CreditCardResponse create(String ownerEmail, CreditCardCreateRequest request) {
        CreditCard card = new CreditCard();
        card.setOwner(currentUserService.findUserByEmail(ownerEmail));
        card.setAccount(resolveAccount(ownerEmail, request.accountId()));
        card.setName(required(request.name(), "O nome do cartao e obrigatorio."));
        card.setBrand(optional(request.brand()));
        card.setLimitAmount(normalizeNullableAmount(request.limitAmount()));
        card.setClosingDay(validDay(request.closingDay()));
        card.setDueDay(validDay(request.dueDay()));
        card.setActive(!Boolean.FALSE.equals(request.active()));
        return CreditCardResponse.from(creditCardRepository.save(card));
    }

    @Transactional
    public CreditCardResponse update(String ownerEmail, Long id, CreditCardUpdateRequest request) {
        CreditCard card = findOwnedCard(ownerEmail, id);
        if (request.accountId() != null) {
            card.setAccount(resolveAccount(ownerEmail, request.accountId()));
        }
        if (request.name() != null) {
            card.setName(required(request.name(), "O nome do cartao e obrigatorio."));
        }
        if (request.brand() != null) {
            card.setBrand(optional(request.brand()));
        }
        if (request.limitAmount() != null) {
            card.setLimitAmount(normalizeNullableAmount(request.limitAmount()));
        }
        if (request.closingDay() != null) {
            card.setClosingDay(validDay(request.closingDay()));
        }
        if (request.dueDay() != null) {
            card.setDueDay(validDay(request.dueDay()));
        }
        if (request.active() != null) {
            card.setActive(request.active());
        }
        return CreditCardResponse.from(card);
    }

    @Transactional
    public void deactivate(String ownerEmail, Long id) {
        findOwnedCard(ownerEmail, id).setActive(false);
    }

    @Transactional(readOnly = true)
    public CreditCard findOwnedCard(String ownerEmail, Long id) {
        return creditCardRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cartao de credito nao encontrado."));
    }

    private Account resolveAccount(String ownerEmail, Long accountId) {
        return accountId == null ? null : accountService.findOwnedAccount(ownerEmail, accountId);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer validDay(Integer day) {
        if (day == null || day < 1 || day > 31) {
            throw new BusinessException("O dia precisa estar entre 1 e 31.");
        }
        return day;
    }

    private BigDecimal normalizeNullableAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.signum() < 0) {
            throw new BusinessException("O limite deve ser maior ou igual a zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
