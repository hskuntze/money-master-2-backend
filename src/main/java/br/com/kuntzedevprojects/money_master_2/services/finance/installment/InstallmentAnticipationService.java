package br.com.kuntzedevprojects.money_master_2.services.finance.installment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationPreviewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseEntryResponse;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipation;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipationItem;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentAnticipationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentMode;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentSource;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentSource;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentAnticipationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseEntryRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoiceService;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;

@Service
public class InstallmentAnticipationService {

    private final InstallmentAnticipationRepository anticipationRepository;
    private final InstallmentPurchaseRepository purchaseRepository;
    private final InstallmentPurchaseEntryRepository entryRepository;
    private final CreditCardInvoiceItemRepository invoiceItemRepository;
    private final CurrentUserService currentUserService;
    private final FinancialPeriodService financialPeriodService;
    private final CreditCardInvoiceService creditCardInvoiceService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public InstallmentAnticipationService(
            InstallmentAnticipationRepository anticipationRepository,
            InstallmentPurchaseRepository purchaseRepository,
            InstallmentPurchaseEntryRepository entryRepository,
            CreditCardInvoiceItemRepository invoiceItemRepository,
            CurrentUserService currentUserService,
            FinancialPeriodService financialPeriodService,
            CreditCardInvoiceService creditCardInvoiceService,
            PaymentService paymentService,
            PaymentRepository paymentRepository
    ) {
        this.anticipationRepository = anticipationRepository;
        this.purchaseRepository = purchaseRepository;
        this.entryRepository = entryRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.currentUserService = currentUserService;
        this.financialPeriodService = financialPeriodService;
        this.creditCardInvoiceService = creditCardInvoiceService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public InstallmentAnticipationPreviewResponse preview(
            String ownerEmail,
            Long purchaseId,
            List<Long> installmentIds,
            BigDecimal discountAmount,
            BigDecimal anticipatedAmount,
            Long targetInvoiceId
    ) {
        InstallmentPurchase purchase = findOwnedPurchase(ownerEmail, purchaseId);
        List<InstallmentPurchaseEntry> entries = selectedEntries(ownerEmail, purchaseId, installmentIds);
        validateEntries(purchase, entries);
        BigDecimal original = sum(entries);
        BigDecimal discount = normalizeZero(discountAmount);
        BigDecimal anticipated = anticipatedAmount == null ? original.subtract(discount) : normalizePositive(anticipatedAmount);
        if (anticipated.signum() <= 0 || anticipated.compareTo(original) > 0) {
            throw new BusinessException("O valor antecipado precisa ser maior que zero e nao pode superar o valor original.");
        }
        FinancialPeriod impactedCycle = financialPeriodService.findOrCreateForDate(ownerEmail, LocalDate.now());
        CreditCardInvoice targetInvoice = resolvePreviewInvoice(ownerEmail, purchase, targetInvoiceId);
        return new InstallmentAnticipationPreviewResponse(
                purchase.getId(),
                purchase.getDescription(),
                purchase.getPaymentMode(),
                entries.stream().map(InstallmentPurchaseEntryResponse::from).toList(),
                original,
                discount,
                anticipated,
                impactedCycle.getId(),
                impactedCycle.getName(),
                targetInvoice == null ? null : targetInvoice.getId(),
                targetInvoice == null ? null : targetInvoiceDescription(targetInvoice),
                entries.stream()
                        .map(InstallmentPurchaseEntry::getInvoiceItem)
                        .filter(item -> item != null && item.getInvoice() != null)
                        .map(item -> item.getInvoice().getId())
                        .distinct()
                        .toList(),
                entries.stream()
                        .map(InstallmentPurchaseEntry::getMonthlyPlanItem)
                        .filter(item -> item != null)
                        .map(MonthlyPlanItem::getId)
                        .toList(),
                purchase.getPaymentMode() == InstallmentPaymentMode.CREDIT_CARD
                        ? "As parcelas selecionadas serao removidas das faturas futuras e somadas na fatura alvo."
                        : "As parcelas selecionadas serao pagas agora e deixarao de aparecer como pendentes nos meses futuros.",
                "installment-anticipation:" + purchase.getId() + ":" + entries.stream().map(e -> String.valueOf(e.getId())).reduce("", (a, b) -> a + "," + b)
        );
    }

    @Transactional
    public InstallmentAnticipationResponse anticipate(String ownerEmail, Long purchaseId, InstallmentAnticipationRequest request) {
        InstallmentPurchase purchase = findOwnedPurchase(ownerEmail, purchaseId);
        List<InstallmentPurchaseEntry> entries = selectedEntries(ownerEmail, purchaseId, request.installmentIds());
        validateEntries(purchase, entries);
        LocalDate anticipationDate = request.anticipationDate() == null ? LocalDate.now() : request.anticipationDate();
        BigDecimal original = sum(entries);
        BigDecimal discount = normalizeZero(request.discountAmount());
        BigDecimal anticipated = request.anticipatedAmount() == null ? original.subtract(discount) : normalizePositive(request.anticipatedAmount());
        if (anticipated.signum() <= 0 || anticipated.compareTo(original) > 0) {
            throw new BusinessException("O valor antecipado precisa ser maior que zero e nao pode superar o valor original.");
        }
        FinancialPeriod cycle = financialPeriodService.findOrCreateForDate(ownerEmail, anticipationDate);

        InstallmentAnticipation anticipation = new InstallmentAnticipation();
        anticipation.setOwner(currentUserService.findUserByEmail(ownerEmail));
        anticipation.setPurchase(purchase);
        anticipation.setCreditCard(purchase.getCreditCard());
        anticipation.setCycle(cycle);
        anticipation.setAnticipationDate(anticipationDate);
        anticipation.setOriginalAmount(original);
        anticipation.setAnticipatedAmount(anticipated);
        anticipation.setDiscountAmount(discount);
        anticipation.setStatus(InstallmentAnticipationStatus.ACTIVE);
        anticipation.setNotes(normalizeNullable(request.notes()));

        if (purchase.getPaymentMode() == InstallmentPaymentMode.CREDIT_CARD) {
            CreditCardInvoice targetInvoice = request.targetInvoiceId() == null
                    ? creditCardInvoiceService.findOrCreateForInstallment(ownerEmail, purchase.getCreditCard().getId(), anticipationDate)
                    : creditCardInvoiceService.findOwnedInvoice(ownerEmail, request.targetInvoiceId());
            anticipation.setTargetInvoice(targetInvoice);
            CreditCardInvoiceItem targetItem = createTargetInvoiceItem(purchase, targetInvoice, anticipationDate, entries, anticipated, request.notes());
            for (InstallmentPurchaseEntry entry : entries) {
                removeFutureInvoiceItem(ownerEmail, entry);
                entry.setInvoiceItem(targetItem);
                markAnticipated(entry, anticipationDate);
                anticipation.addItem(anticipationItem(entry, distributedAmount(entry, entries, anticipated)));
            }
            creditCardInvoiceService.syncInvoiceTotals(ownerEmail, targetInvoice);
        } else {
            List<BigDecimal> distributed = distribute(entries, anticipated);
            for (int index = 0; index < entries.size(); index++) {
                InstallmentPurchaseEntry entry = entries.get(index);
                MonthlyPlanItem payable = entry.getMonthlyPlanItem();
                if (payable == null) {
                    throw new BusinessException("Parcela sem obrigacao mensal nao pode ser antecipada fora do cartao.");
                }
                paymentService.registerPayablePayment(ownerEmail, payable.getId(), new PaymentRequest(
                        null,
                        request.accountId(),
                        payable.getCategory() == null ? null : payable.getCategory().getId(),
                        distributed.get(index),
                        anticipationDate,
                        PaymentMethod.OTHER,
                        PaymentSource.INSTALLMENT_ANTICIPATION,
                        true,
                        appendNote(request.notes(), "Antecipacao de parcela " + entry.getInstallmentNumber() + "/" + purchase.getInstallmentCount())
                ));
                markAnticipated(entry, anticipationDate);
                anticipation.addItem(anticipationItem(entry, distributed.get(index)));
            }
        }

        InstallmentAnticipation saved = anticipationRepository.save(anticipation);
        return InstallmentAnticipationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InstallmentAnticipationResponse> list(String ownerEmail, Long purchaseId) {
        findOwnedPurchase(ownerEmail, purchaseId);
        return anticipationRepository.findByPurchaseIdAndOwnerEmail(ownerEmail, purchaseId)
                .stream()
                .map(InstallmentAnticipationResponse::from)
                .toList();
    }

    @Transactional
    public InstallmentAnticipationResponse cancel(String ownerEmail, Long id) {
        InstallmentAnticipation anticipation = anticipationRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Antecipacao nao encontrada."));
        if (anticipation.getStatus() == InstallmentAnticipationStatus.CANCELED) {
            return InstallmentAnticipationResponse.from(anticipation);
        }
        if (anticipation.getPurchase().getPaymentMode() == InstallmentPaymentMode.CREDIT_CARD) {
            cancelCreditCardAnticipation(ownerEmail, anticipation);
        } else {
            cancelDirectAnticipation(ownerEmail, anticipation);
        }
        anticipation.setStatus(InstallmentAnticipationStatus.CANCELED);
        return InstallmentAnticipationResponse.from(anticipation);
    }

    private void cancelDirectAnticipation(String ownerEmail, InstallmentAnticipation anticipation) {
        for (InstallmentAnticipationItem item : anticipation.getItems()) {
            InstallmentPurchaseEntry entry = item.getInstallment();
            MonthlyPlanItem payable = entry.getMonthlyPlanItem();
            if (payable != null) {
                paymentRepository.findActiveByPayableIdAndSource(payable.getId(), PaymentSource.INSTALLMENT_ANTICIPATION)
                        .forEach(payment -> paymentService.cancel(ownerEmail, payment.getId()));
            }
            entry.setStatus(InstallmentEntryStatus.POSTED);
            entry.setAnticipated(false);
            entry.setAnticipatedAt(null);
            entry.setPaidOn(null);
            entry.setPaymentSource(InstallmentPaymentSource.NONE);
            entry.setPaymentRegisteredAt(null);
        }
    }

    private void cancelCreditCardAnticipation(String ownerEmail, InstallmentAnticipation anticipation) {
        CreditCardInvoice targetInvoice = anticipation.getTargetInvoice();
        CreditCardInvoiceItem targetItem = null;
        for (InstallmentAnticipationItem item : anticipation.getItems()) {
            InstallmentPurchaseEntry entry = item.getInstallment();
            if (targetItem == null) {
                targetItem = entry.getInvoiceItem();
            }
            CreditCardInvoice futureInvoice = creditCardInvoiceService.findOrCreateForInstallment(
                    ownerEmail,
                    anticipation.getPurchase().getCreditCard().getId(),
                    item.getOriginalDueDate()
            );
            CreditCardInvoiceItem restored = new CreditCardInvoiceItem();
            restored.setOwner(anticipation.getOwner());
            restored.setInvoice(futureInvoice);
            restored.setCategory(anticipation.getPurchase().getCategory());
            restored.setDescription(anticipation.getPurchase().getDescription() + " (" + entry.getInstallmentNumber() + "/" + anticipation.getPurchase().getInstallmentCount() + ")");
            restored.setAmount(item.getOriginalAmount());
            restored.setPurchaseDate(anticipation.getPurchase().getPurchaseDate());
            restored.setCompetenceDate(item.getOriginalDueDate());
            restored.setSourceType(CreditCardInvoiceItemSourceType.INSTALLMENT);
            restored.setSourceId(anticipation.getPurchase().getId());
            restored.setInstallmentNumber(entry.getInstallmentNumber());
            restored.setNotes("Parcela restaurada apos cancelamento da antecipacao #" + anticipation.getId() + ".");
            entry.setInvoiceItem(invoiceItemRepository.save(restored));
            entry.setStatus(InstallmentEntryStatus.IN_INVOICE);
            entry.setAnticipated(false);
            entry.setAnticipatedAt(null);
            entry.setPaidOn(null);
            entry.setPaymentSource(InstallmentPaymentSource.NONE);
            entry.setPaymentRegisteredAt(null);
            creditCardInvoiceService.syncInvoiceTotals(ownerEmail, futureInvoice);
        }
        if (targetItem != null) {
            invoiceItemRepository.delete(targetItem);
            invoiceItemRepository.flush();
        }
        if (targetInvoice != null) {
            creditCardInvoiceService.syncInvoiceTotals(ownerEmail, targetInvoice);
        }
    }

    private InstallmentPurchase findOwnedPurchase(String ownerEmail, Long purchaseId) {
        return purchaseRepository.findByIdAndOwnerEmailWithEntries(purchaseId, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Compra parcelada nao encontrada."));
    }

    private List<InstallmentPurchaseEntry> selectedEntries(String ownerEmail, Long purchaseId, List<Long> installmentIds) {
        if (installmentIds == null || installmentIds.isEmpty()) {
            throw new BusinessException("Selecione ao menos uma parcela para antecipar.");
        }
        List<InstallmentPurchaseEntry> entries = entryRepository.findSelectedByPurchaseAndOwnerEmail(ownerEmail, purchaseId, installmentIds);
        if (entries.size() != installmentIds.size()) {
            throw new BusinessException("Uma ou mais parcelas selecionadas nao foram encontradas.");
        }
        return entries;
    }

    private void validateEntries(InstallmentPurchase purchase, List<InstallmentPurchaseEntry> entries) {
        if (purchase.getPaymentMode() == InstallmentPaymentMode.CREDIT_CARD && purchase.getCreditCard() == null) {
            throw new BusinessException("Compra no cartao sem cartao vinculado.");
        }
        for (InstallmentPurchaseEntry entry : entries) {
            if (entry.getStatus() == InstallmentEntryStatus.PAID) {
                throw new BusinessException("Nao e possivel antecipar parcela ja paga.");
            }
            if (entry.getStatus() == InstallmentEntryStatus.ANTICIPATED || entry.isAnticipated()) {
                throw new BusinessException("Nao e possivel antecipar parcela ja antecipada.");
            }
            if (entry.getStatus() == InstallmentEntryStatus.CANCELED) {
                throw new BusinessException("Nao e possivel antecipar parcela cancelada.");
            }
        }
    }

    private CreditCardInvoice resolvePreviewInvoice(String ownerEmail, InstallmentPurchase purchase, Long targetInvoiceId) {
        if (purchase.getPaymentMode() != InstallmentPaymentMode.CREDIT_CARD) {
            return null;
        }
        if (targetInvoiceId != null) {
            return creditCardInvoiceService.findOwnedInvoice(ownerEmail, targetInvoiceId);
        }
        return null;
    }

    private CreditCardInvoiceItem createTargetInvoiceItem(
            InstallmentPurchase purchase,
            CreditCardInvoice invoice,
            LocalDate anticipationDate,
            List<InstallmentPurchaseEntry> entries,
            BigDecimal amount,
            String notes
    ) {
        CreditCardInvoiceItem item = new CreditCardInvoiceItem();
        item.setOwner(purchase.getOwner());
        item.setInvoice(invoice);
        item.setCategory(purchase.getCategory());
        item.setDescription("Antecipacao " + purchase.getDescription() + " parcelas " + installmentRange(entries));
        item.setAmount(amount);
        item.setPurchaseDate(anticipationDate);
        item.setCompetenceDate(anticipationDate);
        item.setSourceType(CreditCardInvoiceItemSourceType.INSTALLMENT_ANTICIPATION);
        item.setSourceId(purchase.getId());
        item.setNotes(normalizeNullable(notes));
        return invoiceItemRepository.save(item);
    }

    private void removeFutureInvoiceItem(String ownerEmail, InstallmentPurchaseEntry entry) {
        CreditCardInvoiceItem invoiceItem = entry.getInvoiceItem();
        if (invoiceItem == null) {
            return;
        }
        CreditCardInvoice invoice = invoiceItem.getInvoice();
        invoiceItemRepository.delete(invoiceItem);
        invoiceItemRepository.flush();
        entry.setInvoiceItem(null);
        creditCardInvoiceService.syncInvoiceTotals(ownerEmail, invoice);
    }

    private void markAnticipated(InstallmentPurchaseEntry entry, LocalDate anticipationDate) {
        entry.setStatus(InstallmentEntryStatus.ANTICIPATED);
        entry.setAnticipated(true);
        entry.setAnticipatedAt(Instant.now());
        entry.setPaidOn(anticipationDate);
        entry.setPaymentSource(InstallmentPaymentSource.MANUAL);
        entry.setPaymentRegisteredAt(Instant.now());
    }

    private InstallmentAnticipationItem anticipationItem(InstallmentPurchaseEntry entry, BigDecimal anticipatedAmount) {
        InstallmentAnticipationItem item = new InstallmentAnticipationItem();
        item.setInstallment(entry);
        item.setOriginalDueDate(entry.getDueDate());
        item.setOriginalAmount(normalizeZero(entry.getAmount()));
        item.setAnticipatedAmount(anticipatedAmount);
        return item;
    }

    private List<BigDecimal> distribute(List<InstallmentPurchaseEntry> entries, BigDecimal total) {
        BigDecimal original = sum(entries);
        List<BigDecimal> values = new ArrayList<>();
        BigDecimal accumulated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (int index = 0; index < entries.size(); index++) {
            BigDecimal amount = index == entries.size() - 1
                    ? total.subtract(accumulated).setScale(2, RoundingMode.HALF_UP)
                    : normalizeZero(entries.get(index).getAmount()).multiply(total).divide(original, 2, RoundingMode.HALF_UP);
            values.add(amount);
            accumulated = accumulated.add(amount);
        }
        return values;
    }

    private BigDecimal distributedAmount(InstallmentPurchaseEntry entry, List<InstallmentPurchaseEntry> entries, BigDecimal total) {
        return distribute(entries, total).get(entries.indexOf(entry));
    }

    private BigDecimal sum(List<InstallmentPurchaseEntry> entries) {
        return entries.stream().map(InstallmentPurchaseEntry::getAmount).map(this::normalizeZero).reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    private BigDecimal normalizeZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("O valor antecipado deve ser maior que zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String targetInvoiceDescription(CreditCardInvoice invoice) {
        return "Fatura " + invoice.getCreditCard().getName() + " - " + String.format("%02d/%04d", invoice.getReferenceMonth(), invoice.getReferenceYear());
    }

    private String installmentRange(List<InstallmentPurchaseEntry> entries) {
        return entries.stream().map(entry -> String.valueOf(entry.getInstallmentNumber())).reduce((a, b) -> a + "," + b).orElse("");
    }

    private String appendNote(String note, String extra) {
        String normalized = normalizeNullable(note);
        return normalized == null ? extra : normalized + "\n" + extra;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
