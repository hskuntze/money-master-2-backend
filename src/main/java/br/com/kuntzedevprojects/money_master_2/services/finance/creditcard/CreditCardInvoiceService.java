package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class CreditCardInvoiceService {

    private final CreditCardInvoiceRepository invoiceRepository;
    private final CreditCardInvoiceItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final CreditCardService creditCardService;
    private final CurrentUserService currentUserService;
    private final FinancialPeriodService financialPeriodService;

    public CreditCardInvoiceService(
            CreditCardInvoiceRepository invoiceRepository,
            CreditCardInvoiceItemRepository itemRepository,
            PaymentRepository paymentRepository,
            CreditCardService creditCardService,
            CurrentUserService currentUserService,
            FinancialPeriodService financialPeriodService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.creditCardService = creditCardService;
        this.currentUserService = currentUserService;
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<CreditCardInvoiceResponse> search(String ownerEmail, Long cardId, Long cycleId) {
        if (cardId != null) {
            creditCardService.findOwnedCard(ownerEmail, cardId);
        }
        if (cycleId != null) {
            financialPeriodService.findOwnedPeriod(ownerEmail, cycleId);
        }
        return invoiceRepository.search(ownerEmail, cardId, cycleId).stream()
                .map(CreditCardInvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCardInvoiceResponse get(String ownerEmail, Long id) {
        return CreditCardInvoiceResponse.from(findOwnedInvoice(ownerEmail, id));
    }

    @Transactional
    public CreditCardInvoiceResponse create(String ownerEmail, Long cardId, CreditCardInvoiceCreateRequest request) {
        CreditCard card = creditCardService.findOwnedCard(ownerEmail, cardId);
        FinancialPeriod cycle = financialPeriodService.findOwnedPeriod(ownerEmail, request.cycleId());
        YearMonth reference = YearMonth.of(request.referenceYear(), request.referenceMonth());
        LocalDate closingDate = request.closingDate() == null ? cardDate(reference, card.getClosingDay()) : request.closingDate();
        LocalDate dueDate = request.dueDate() == null ? resolveDueDate(reference, closingDate, card.getDueDay()) : request.dueDate();

        CreditCardInvoice invoice = new CreditCardInvoice();
        invoice.setOwner(currentUserService.findUserByEmail(ownerEmail));
        invoice.setCreditCard(card);
        invoice.setCycle(cycle);
        invoice.setReferenceMonth(request.referenceMonth());
        invoice.setReferenceYear(request.referenceYear());
        invoice.setOpeningDate(request.openingDate());
        invoice.setClosingDate(closingDate);
        invoice.setDueDate(dueDate);
        invoice.setStatus(CreditCardInvoiceStatus.OPEN);
        invoice.setExpectedAmount(zero());
        invoice.setFinalAmount(zero());
        invoice.setPaidAmount(zero());
        CreditCardInvoice saved = invoiceRepository.save(invoice);
        saved.setMonthlyPayable(createMonthlyPayable(ownerEmail, saved));
        return CreditCardInvoiceResponse.from(saved);
    }

    @Transactional
    public CreditCardInvoice findOrCreateForInstallment(String ownerEmail, Long cardId, LocalDate dueDate) {
        CreditCard card = creditCardService.findOwnedCard(ownerEmail, cardId);
        FinancialPeriod cycle = financialPeriodService.findOrCreateForDate(ownerEmail, dueDate);
        List<CreditCardInvoice> existing = invoiceRepository.search(ownerEmail, cardId, cycle.getId());
        return existing.stream()
                .filter(invoice -> invoice.getReferenceMonth().equals(dueDate.getMonthValue()))
                .filter(invoice -> invoice.getReferenceYear().equals(dueDate.getYear()))
                .findFirst()
                .orElseGet(() -> {
                    LocalDate closingDate = dueDate.minusDays(Math.max(1, card.getDueDay() - card.getClosingDay()));
                    CreditCardInvoiceResponse created = create(ownerEmail, cardId, new CreditCardInvoiceCreateRequest(
                            cycle.getId(),
                            dueDate.getMonthValue(),
                            dueDate.getYear(),
                            null,
                            closingDate,
                            dueDate
                    ));
                    return findOwnedInvoice(ownerEmail, created.id());
                });
    }

    @Transactional
    public CreditCardInvoiceResponse update(String ownerEmail, Long id, CreditCardInvoiceUpdateRequest request) {
        CreditCardInvoice invoice = findOwnedInvoice(ownerEmail, id);
        if (request.openingDate() != null) {
            invoice.setOpeningDate(request.openingDate());
        }
        if (request.closingDate() != null) {
            invoice.setClosingDate(request.closingDate());
        }
        if (request.dueDate() != null) {
            invoice.setDueDate(request.dueDate());
        }
        if (request.status() != null) {
            invoice.setStatus(request.status());
        }
        if (request.finalAmount() != null) {
            invoice.setFinalAmount(normalizeZeroOrPositive(request.finalAmount()));
        }
        syncMonthlyPayable(ownerEmail, invoice);
        refreshPaymentState(invoice);
        return CreditCardInvoiceResponse.from(invoice);
    }

    @Transactional
    public CreditCardInvoiceResponse close(String ownerEmail, Long id) {
        CreditCardInvoice invoice = findOwnedInvoice(ownerEmail, id);
        syncInvoiceTotals(ownerEmail, invoice);
        invoice.setStatus(CreditCardInvoiceStatus.CLOSED);
        invoice.setClosedAt(Instant.now());
        return CreditCardInvoiceResponse.from(invoice);
    }

    @Transactional
    public CreditCardInvoiceResponse reopen(String ownerEmail, Long id) {
        CreditCardInvoice invoice = findOwnedInvoice(ownerEmail, id);
        if (invoice.getStatus() == CreditCardInvoiceStatus.PAID) {
            throw new BusinessException("Nao e possivel reabrir uma fatura ja paga.");
        }
        invoice.setStatus(CreditCardInvoiceStatus.OPEN);
        invoice.setClosedAt(null);
        return CreditCardInvoiceResponse.from(invoice);
    }

    @Transactional
    public void syncInvoiceTotals(String ownerEmail, CreditCardInvoice invoice) {
        BigDecimal total = normalizeZeroOrPositive(itemRepository.sumByInvoiceId(invoice.getId()));
        invoice.setExpectedAmount(total);
        invoice.setFinalAmount(total);
        syncMonthlyPayable(ownerEmail, invoice);
        refreshPaymentState(invoice);
    }

    @Transactional(readOnly = true)
    public CreditCardInvoice findOwnedInvoice(String ownerEmail, Long id) {
        return invoiceRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura de cartao nao encontrada."));
    }

    private MonthlyPlanItem createMonthlyPayable(String ownerEmail, CreditCardInvoice invoice) {
        MonthlyPlanItemCreateRequest request = new MonthlyPlanItemCreateRequest(
                invoice.getCreditCard().getAccount() == null ? null : invoice.getCreditCard().getAccount().getId(),
                null,
                TransactionType.EXPENSE,
                invoiceDescription(invoice),
                invoice.getExpectedAmount(),
                invoice.getDueDate(),
                MonthlyPlanItemNature.CREDIT_CARD,
                MonthlyPlanItemAggregationType.GROUP_PARENT,
                null,
                false,
                null,
                MonthlyPlanItemStatus.PENDING,
                null,
                "Gerado automaticamente pela fatura de cartao #" + invoice.getId()
        );
        MonthlyPlanItemResponse response = financialPeriodService.createPlanItem(ownerEmail, invoice.getCycle().getId(), request);
        return financialPeriodService.findOwnedPlanItem(ownerEmail, response.id());
    }

    private void syncMonthlyPayable(String ownerEmail, CreditCardInvoice invoice) {
        if (invoice.getMonthlyPayable() == null) {
            invoice.setMonthlyPayable(createMonthlyPayable(ownerEmail, invoice));
            return;
        }
        MonthlyPlanItemUpdateRequest request = new MonthlyPlanItemUpdateRequest(
                invoice.getCreditCard().getAccount() == null ? null : invoice.getCreditCard().getAccount().getId(),
                null,
                TransactionType.EXPENSE,
                invoiceDescription(invoice),
                invoice.getFinalAmount(),
                null,
                invoice.getDueDate(),
                null,
                MonthlyPlanItemNature.CREDIT_CARD,
                MonthlyPlanItemAggregationType.GROUP_PARENT,
                null,
                false,
                null,
                null,
                null,
                "Gerado automaticamente pela fatura de cartao #" + invoice.getId()
        );
        financialPeriodService.updatePlanItem(ownerEmail, invoice.getMonthlyPayable().getId(), request);
    }

    void refreshPaymentState(CreditCardInvoice invoice) {
        if (invoice.getMonthlyPayable() == null) {
            invoice.setPaidAmount(zero());
            return;
        }
        BigDecimal paidAmount = normalizeZeroOrPositive(paymentRepository.sumActiveByPayable(invoice.getMonthlyPayable().getId()));
        invoice.setPaidAmount(paidAmount);
        BigDecimal finalAmount = normalizeZeroOrPositive(invoice.getFinalAmount());
        if (paidAmount.signum() <= 0) {
            invoice.setPaidAt(null);
            if (invoice.getStatus() != CreditCardInvoiceStatus.OPEN && invoice.getStatus() != CreditCardInvoiceStatus.CLOSED) {
                invoice.setStatus(invoice.getDueDate().isBefore(LocalDate.now()) ? CreditCardInvoiceStatus.OVERDUE : CreditCardInvoiceStatus.CLOSED);
            }
            return;
        }
        if (paidAmount.compareTo(finalAmount) >= 0) {
            invoice.setStatus(CreditCardInvoiceStatus.PAID);
            invoice.setPaidAt(Instant.now());
            return;
        }
        invoice.setStatus(CreditCardInvoiceStatus.PARTIALLY_PAID);
        invoice.setPaidAt(null);
    }

    private LocalDate cardDate(YearMonth reference, Integer day) {
        return reference.atDay(Math.min(day, reference.lengthOfMonth()));
    }

    private LocalDate resolveDueDate(YearMonth reference, LocalDate closingDate, Integer day) {
        LocalDate candidate = cardDate(reference, day);
        if (!candidate.isAfter(closingDate)) {
            candidate = cardDate(reference.plusMonths(1), day);
        }
        return candidate;
    }

    private String invoiceDescription(CreditCardInvoice invoice) {
        return "Fatura " + invoice.getCreditCard().getName() + " - "
                + String.format("%02d/%04d", invoice.getReferenceMonth(), invoice.getReferenceYear());
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeZeroOrPositive(BigDecimal amount) {
        if (amount == null) {
            return zero();
        }
        if (amount.signum() < 0) {
            throw new BusinessException("O valor deve ser maior ou igual a zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
