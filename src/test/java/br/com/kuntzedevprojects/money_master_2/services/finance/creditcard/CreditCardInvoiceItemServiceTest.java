package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemConfirmationCandidateResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemConfirmRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemConfirmationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;

class CreditCardInvoiceItemServiceTest {

    private final CreditCardInvoiceItemRepository itemRepository = mock(CreditCardInvoiceItemRepository.class);
    private final CreditCardInvoiceService invoiceService = mock(CreditCardInvoiceService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final FinancialTransactionService transactionService = mock(FinancialTransactionService.class);

    private final CreditCardInvoiceItemService service = new CreditCardInvoiceItemService(
            itemRepository,
            invoiceService,
            currentUserService,
            categoryService,
            transactionService
    );

    @Test
    void shouldCreateCardPurchaseItemWithStructuralTransactionLink() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        Category category = category();
        FinancialTransaction transaction = transaction(owner, category);
        when(invoiceService.findOwnedInvoice(owner.getEmail(), invoice.getId())).thenReturn(invoice);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(transactionService.findOwnedTransaction(owner.getEmail(), transaction.getId())).thenReturn(transaction);
        when(itemRepository.existsByTransactionIdExcludingItem(transaction.getId(), null)).thenReturn(false);
        when(itemRepository.save(any(CreditCardInvoiceItem.class))).thenAnswer(invocation -> {
            CreditCardInvoiceItem item = invocation.getArgument(0);
            item.setId(99L);
            return item;
        });

        CreditCardInvoiceItemCreateRequest request = new CreditCardInvoiceItemCreateRequest(
                null,
                "Mercado",
                new BigDecimal("123.45"),
                LocalDate.of(2026, 7, 10),
                null,
                null,
                CreditCardInvoiceItemConfirmationStatus.PLANNED,
                null,
                null,
                transaction.getId(),
                null
        );

        CreditCardInvoiceItemResponse response = service.create(owner.getEmail(), invoice.getId(), request);

        ArgumentCaptor<CreditCardInvoiceItem> captor = ArgumentCaptor.forClass(CreditCardInvoiceItem.class);
        verify(itemRepository).save(captor.capture());
        CreditCardInvoiceItem saved = captor.getValue();
        assertThat(saved.getSourceType()).isEqualTo(CreditCardInvoiceItemSourceType.CARD_PURCHASE);
        assertThat(saved.getConfirmationStatus()).isEqualTo(CreditCardInvoiceItemConfirmationStatus.CONFIRMED);
        assertThat(saved.getTransaction()).isSameAs(transaction);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(response.transactionId()).isEqualTo(transaction.getId());
        assertThat(response.confirmationStatus()).isEqualTo(CreditCardInvoiceItemConfirmationStatus.CONFIRMED);
        assertThat(response.confirmed()).isTrue();
        verify(transactionService).markAsCreditCardInvoiceItem(owner.getEmail(), transaction);
        verify(invoiceService).syncInvoiceTotals(owner.getEmail(), invoice);
    }

    @Test
    void shouldCreatePlannedInvoiceItemWithoutTransactionLink() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        when(invoiceService.findOwnedInvoice(owner.getEmail(), invoice.getId())).thenReturn(invoice);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(itemRepository.save(any(CreditCardInvoiceItem.class))).thenAnswer(invocation -> {
            CreditCardInvoiceItem item = invocation.getArgument(0);
            item.setId(100L);
            return item;
        });

        CreditCardInvoiceItemCreateRequest request = new CreditCardInvoiceItemCreateRequest(
                null,
                "Xbox",
                new BigDecimal("49.90"),
                LocalDate.of(2026, 7, 10),
                null,
                CreditCardInvoiceItemSourceType.CARD_PURCHASE,
                CreditCardInvoiceItemConfirmationStatus.PLANNED,
                null,
                null,
                null,
                null
        );

        CreditCardInvoiceItemResponse response = service.create(owner.getEmail(), invoice.getId(), request);

        ArgumentCaptor<CreditCardInvoiceItem> captor = ArgumentCaptor.forClass(CreditCardInvoiceItem.class);
        verify(itemRepository).save(captor.capture());
        CreditCardInvoiceItem saved = captor.getValue();
        assertThat(saved.getConfirmationStatus()).isEqualTo(CreditCardInvoiceItemConfirmationStatus.PLANNED);
        assertThat(saved.getTransaction()).isNull();
        assertThat(response.confirmationStatus()).isEqualTo(CreditCardInvoiceItemConfirmationStatus.PLANNED);
        assertThat(response.confirmed()).isFalse();
        verify(invoiceService).syncInvoiceTotals(owner.getEmail(), invoice);
    }

    @Test
    void shouldListUnlinkedCreditCardTransactionsAsConfirmationCandidates() {
        User owner = owner();
        Account cardAccount = account(owner);
        Category category = category();
        CreditCardInvoice invoice = invoice(cardAccount);
        CreditCardInvoiceItem item = plannedItem(owner, invoice);
        item.setCategory(category);
        FinancialTransaction transaction = transaction(owner, category);
        transaction.setDescription("Xbox");
        transaction.setAccount(cardAccount);
        when(itemRepository.findByIdAndOwnerEmail(item.getId(), owner.getEmail())).thenReturn(Optional.of(item));
        when(transactionService.search(
                owner.getEmail(),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                cardAccount.getId(),
                null,
                TransactionType.EXPENSE,
                null,
                null
        )).thenReturn(List.of(FinancialTransactionResponse.from(transaction)));

        List<CreditCardInvoiceItemConfirmationCandidateResponse> candidates =
                service.confirmationCandidates(owner.getEmail(), item.getId());

        assertThat(candidates).hasSize(1);
        CreditCardInvoiceItemConfirmationCandidateResponse candidate = candidates.get(0);
        assertThat(candidate.transaction().id()).isEqualTo(transaction.getId());
        assertThat(candidate.score()).isEqualTo(100);
        assertThat(candidate.amountMatches()).isTrue();
        assertThat(candidate.dateMatches()).isTrue();
        assertThat(candidate.categoryMatches()).isTrue();
        assertThat(candidate.reasons()).containsExactly(
                "Mesmo valor",
                "Mesma data",
                "Mesma categoria",
                "Descricao parecida"
        );
    }

    @Test
    void shouldConfirmPlannedInvoiceItemWithoutCreatingAnotherItem() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        CreditCardInvoiceItem item = plannedItem(owner, invoice);
        when(itemRepository.findByIdAndOwnerEmail(item.getId(), owner.getEmail())).thenReturn(Optional.of(item));

        CreditCardInvoiceItemResponse response = service.confirm(owner.getEmail(), item.getId(), null);

        assertThat(item.getConfirmationStatus()).isEqualTo(CreditCardInvoiceItemConfirmationStatus.CONFIRMED);
        assertThat(item.getTransaction()).isNull();
        assertThat(response.id()).isEqualTo(item.getId());
        assertThat(response.confirmed()).isTrue();
        verify(invoiceService).syncInvoiceTotals(owner.getEmail(), invoice);
    }

    @Test
    void shouldConfirmPlannedInvoiceItemAndLinkRealTransaction() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        Category category = category();
        CreditCardInvoiceItem item = plannedItem(owner, invoice);
        FinancialTransaction transaction = transaction(owner, category);
        when(itemRepository.findByIdAndOwnerEmail(item.getId(), owner.getEmail())).thenReturn(Optional.of(item));
        when(transactionService.findOwnedTransaction(owner.getEmail(), transaction.getId())).thenReturn(transaction);
        when(itemRepository.existsByTransactionIdExcludingItem(transaction.getId(), item.getId())).thenReturn(false);

        CreditCardInvoiceItemResponse response = service.confirm(
                owner.getEmail(),
                item.getId(),
                new CreditCardInvoiceItemConfirmRequest(transaction.getId(), null, null, null, "Confirmado na fatura")
        );

        assertThat(item.getConfirmationStatus()).isEqualTo(CreditCardInvoiceItemConfirmationStatus.CONFIRMED);
        assertThat(item.getTransaction()).isSameAs(transaction);
        assertThat(item.getCategory()).isSameAs(category);
        assertThat(item.getNotes()).isEqualTo("Confirmado na fatura");
        assertThat(response.transactionId()).isEqualTo(transaction.getId());
        assertThat(response.confirmed()).isTrue();
        verify(transactionService).markAsCreditCardInvoiceItem(owner.getEmail(), transaction);
        verify(invoiceService).syncInvoiceTotals(owner.getEmail(), invoice);
    }

    @Test
    void shouldRejectTransactionAlreadyLinkedToAnotherInvoiceItem() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        when(invoiceService.findOwnedInvoice(owner.getEmail(), invoice.getId())).thenReturn(invoice);
        when(itemRepository.existsByTransactionIdExcludingItem(55L, null)).thenReturn(true);

        CreditCardInvoiceItemCreateRequest request = new CreditCardInvoiceItemCreateRequest(
                null,
                "Mercado",
                new BigDecimal("123.45"),
                LocalDate.of(2026, 7, 10),
                null,
                CreditCardInvoiceItemSourceType.CARD_PURCHASE,
                null,
                null,
                null,
                55L,
                null
        );

        assertThatThrownBy(() -> service.create(owner.getEmail(), invoice.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ja esta vinculada");
    }

    private User owner() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");
        owner.setName("User");
        return owner;
    }

    private CreditCardInvoice invoice() {
        CreditCardInvoice invoice = new CreditCardInvoice();
        invoice.setId(10L);
        return invoice;
    }

    private CreditCardInvoice invoice(Account cardAccount) {
        CreditCard card = new CreditCard();
        card.setId(30L);
        card.setName("Nubank");
        card.setAccount(cardAccount);

        CreditCardInvoice invoice = invoice();
        invoice.setCreditCard(card);
        invoice.setOpeningDate(LocalDate.of(2026, 7, 1));
        invoice.setClosingDate(LocalDate.of(2026, 7, 31));
        return invoice;
    }

    private Account account(User owner) {
        Account account = new Account();
        account.setId(20L);
        account.setOwner(owner);
        account.setName("Cartao Nubank");
        account.setType(AccountType.CREDIT_CARD);
        return account;
    }

    private Category category() {
        Category category = new Category();
        category.setId(20L);
        category.setName("Mercado");
        category.setType(TransactionType.EXPENSE);
        return category;
    }

    private FinancialTransaction transaction(User owner, Category category) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setId(55L);
        transaction.setOwner(owner);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("123.45"));
        transaction.setOccurredOn(LocalDate.of(2026, 7, 10));
        transaction.setCategory(category);
        return transaction;
    }

    private CreditCardInvoiceItem plannedItem(User owner, CreditCardInvoice invoice) {
        CreditCardInvoiceItem item = new CreditCardInvoiceItem();
        item.setId(77L);
        item.setOwner(owner);
        item.setInvoice(invoice);
        item.setDescription("Xbox");
        item.setAmount(new BigDecimal("123.45"));
        item.setPurchaseDate(LocalDate.of(2026, 7, 10));
        item.setCompetenceDate(LocalDate.of(2026, 7, 10));
        item.setSourceType(CreditCardInvoiceItemSourceType.CARD_PURCHASE);
        item.setConfirmationStatus(CreditCardInvoiceItemConfirmationStatus.PLANNED);
        return item;
    }
}
