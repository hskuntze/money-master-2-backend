package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceCreateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

class CreditCardInvoiceServiceTest {

    private final CreditCardInvoiceRepository invoiceRepository = mock(CreditCardInvoiceRepository.class);
    private final CreditCardInvoiceItemRepository itemRepository = mock(CreditCardInvoiceItemRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final CreditCardService creditCardService = mock(CreditCardService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);

    private final CreditCardInvoiceService service = new CreditCardInvoiceService(
            invoiceRepository,
            itemRepository,
            paymentRepository,
            creditCardService,
            currentUserService,
            financialPeriodService
    );

    @Test
    void shouldCreateInvoiceWithAutomaticallyGeneratedMonthlyPayable() {
        User owner = owner();
        FinancialPeriod cycle = cycle(owner);
        CreditCard card = card(owner, account(owner));
        MonthlyPlanItem payable = invoicePayable(owner, cycle, card);
        when(creditCardService.findOwnedCard(owner.getEmail(), card.getId())).thenReturn(card);
        when(financialPeriodService.findOwnedPeriod(owner.getEmail(), cycle.getId())).thenReturn(cycle);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(invoiceRepository.save(any(CreditCardInvoice.class))).thenAnswer(invocation -> {
            CreditCardInvoice invoice = invocation.getArgument(0);
            invoice.setId(90L);
            return invoice;
        });
        when(financialPeriodService.createPlanItem(eq(owner.getEmail()), eq(cycle.getId()), any(MonthlyPlanItemCreateRequest.class)))
                .thenReturn(MonthlyPlanItemResponse.from(payable));
        when(financialPeriodService.findOwnedPlanItem(owner.getEmail(), payable.getId())).thenReturn(payable);

        var response = service.create(owner.getEmail(), card.getId(), new CreditCardInvoiceCreateRequest(
                cycle.getId(),
                9,
                2026,
                LocalDate.of(2026, 8, 11),
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 10, 10)
        ));

        ArgumentCaptor<MonthlyPlanItemCreateRequest> request = ArgumentCaptor.forClass(MonthlyPlanItemCreateRequest.class);
        verify(financialPeriodService).createPlanItem(eq(owner.getEmail()), eq(cycle.getId()), request.capture());
        assertAll(
                () -> assertThat(response.monthlyPayableId()).isEqualTo(payable.getId()),
                () -> assertThat(response.status()).isEqualTo(CreditCardInvoiceStatus.OPEN),
                () -> assertThat(response.expectedAmount()).isEqualByComparingTo("0.00"),
                () -> assertThat(response.finalAmount()).isEqualByComparingTo("0.00"),
                () -> assertThat(response.paidAmount()).isEqualByComparingTo("0.00"),
                () -> assertThat(request.getValue().type()).isEqualTo(TransactionType.EXPENSE),
                () -> assertThat(request.getValue().description()).isEqualTo("Fatura Nubank - 09/2026"),
                () -> assertThat(request.getValue().expectedAmount()).isEqualByComparingTo("0.00"),
                () -> assertThat(request.getValue().dueDate()).isEqualTo(LocalDate.of(2026, 10, 10)),
                () -> assertThat(request.getValue().nature()).isEqualTo(MonthlyPlanItemNature.CREDIT_CARD),
                () -> assertThat(request.getValue().aggregationType()).isEqualTo(MonthlyPlanItemAggregationType.GROUP_PARENT)
        );
    }

    @Test
    void shouldSyncInvoiceTotalsIntoLegacyMonthlyPayableAndRefreshPartialPaymentState() {
        User owner = owner();
        FinancialPeriod cycle = cycle(owner);
        CreditCard card = card(owner, account(owner));
        MonthlyPlanItem payable = invoicePayable(owner, cycle, card);
        CreditCardInvoice invoice = invoice(owner, cycle, card, payable);
        when(itemRepository.sumByInvoiceId(invoice.getId())).thenReturn(new BigDecimal("537.76"));
        when(paymentRepository.sumActiveByPayable(payable.getId())).thenReturn(new BigDecimal("200.00"));

        service.syncInvoiceTotals(owner.getEmail(), invoice);

        ArgumentCaptor<MonthlyPlanItemUpdateRequest> request = ArgumentCaptor.forClass(MonthlyPlanItemUpdateRequest.class);
        verify(financialPeriodService).updatePlanItem(eq(owner.getEmail()), eq(payable.getId()), request.capture());
        assertAll(
                () -> assertThat(invoice.getExpectedAmount()).isEqualByComparingTo("537.76"),
                () -> assertThat(invoice.getFinalAmount()).isEqualByComparingTo("537.76"),
                () -> assertThat(invoice.getPaidAmount()).isEqualByComparingTo("200.00"),
                () -> assertThat(invoice.getStatus()).isEqualTo(CreditCardInvoiceStatus.PARTIALLY_PAID),
                () -> assertThat(request.getValue().description()).isEqualTo("Fatura Nubank - 09/2026"),
                () -> assertThat(request.getValue().expectedAmount()).isEqualByComparingTo("537.76"),
                () -> assertThat(request.getValue().dueDate()).isEqualTo(LocalDate.of(2026, 10, 10)),
                () -> assertThat(request.getValue().nature()).isEqualTo(MonthlyPlanItemNature.CREDIT_CARD),
                () -> assertThat(request.getValue().aggregationType()).isEqualTo(MonthlyPlanItemAggregationType.GROUP_PARENT)
        );
    }

    private User owner() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Ana");
        owner.setEmail("ana@example.com");
        return owner;
    }

    private Account account(User owner) {
        Account account = new Account();
        account.setId(20L);
        account.setOwner(owner);
        account.setName("Conta Nubank");
        account.setType(AccountType.CHECKING);
        return account;
    }

    private CreditCard card(User owner, Account account) {
        CreditCard card = new CreditCard();
        card.setId(30L);
        card.setOwner(owner);
        card.setAccount(account);
        card.setName("Nubank");
        card.setClosingDay(10);
        card.setDueDay(10);
        return card;
    }

    private FinancialPeriod cycle(User owner) {
        FinancialPeriod cycle = new FinancialPeriod();
        cycle.setId(40L);
        cycle.setOwner(owner);
        cycle.setName("Outubro 2026");
        cycle.setStartDate(LocalDate.of(2026, 10, 1));
        cycle.setEndDate(LocalDate.of(2026, 10, 31));
        cycle.setStatus(FinancialPeriodStatus.OPEN);
        return cycle;
    }

    private MonthlyPlanItem invoicePayable(User owner, FinancialPeriod cycle, CreditCard card) {
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setId(50L);
        item.setOwner(owner);
        item.setFinancialPeriod(cycle);
        item.setAccount(card.getAccount());
        item.setType(TransactionType.EXPENSE);
        item.setDescription("Fatura Nubank - 09/2026");
        item.setExpectedAmount(BigDecimal.ZERO);
        item.setActualAmount(BigDecimal.ZERO);
        item.setDueDate(LocalDate.of(2026, 10, 10));
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(MonthlyPlanItemNature.CREDIT_CARD);
        item.setAggregationType(MonthlyPlanItemAggregationType.GROUP_PARENT);
        return item;
    }

    private CreditCardInvoice invoice(User owner, FinancialPeriod cycle, CreditCard card, MonthlyPlanItem payable) {
        CreditCardInvoice invoice = new CreditCardInvoice();
        invoice.setId(90L);
        invoice.setOwner(owner);
        invoice.setCreditCard(card);
        invoice.setCycle(cycle);
        invoice.setMonthlyPayable(payable);
        invoice.setReferenceMonth(9);
        invoice.setReferenceYear(2026);
        invoice.setOpeningDate(LocalDate.of(2026, 8, 11));
        invoice.setClosingDate(LocalDate.of(2026, 9, 10));
        invoice.setDueDate(LocalDate.of(2026, 10, 10));
        invoice.setStatus(CreditCardInvoiceStatus.CLOSED);
        invoice.setExpectedAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);
        return invoice;
    }
}
