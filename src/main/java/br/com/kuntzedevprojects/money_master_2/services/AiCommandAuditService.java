package br.com.kuntzedevprojects.money_master_2.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditReverseRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditReverseResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditReversalEventResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandReversalResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandResult;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentReverseRequest;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAuditReversal;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandReversalStatus;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentAnticipationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditReversalRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentAnticipationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;

@Service
public class AiCommandAuditService {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final AiCommandAuditRepository repository;
    private final AiCommandAuditReversalRepository reversalRepository;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final InstallmentAnticipationRepository anticipationRepository;
    private final PaymentService paymentService;
    private final InstallmentAnticipationService anticipationService;

    public AiCommandAuditService(
            AiCommandAuditRepository repository,
            AiCommandAuditReversalRepository reversalRepository,
            ObjectMapper objectMapper,
            PaymentRepository paymentRepository,
            InstallmentAnticipationRepository anticipationRepository,
            PaymentService paymentService,
            InstallmentAnticipationService anticipationService
    ) {
        this.repository = repository;
        this.reversalRepository = reversalRepository;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.anticipationRepository = anticipationRepository;
        this.paymentService = paymentService;
        this.anticipationService = anticipationService;
    }

    @Transactional(readOnly = true)
    public List<AiCommandAuditResponse> listRecent(String ownerEmail, Integer limit) {
        int pageSize = normalizeLimit(limit);
        return repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(ownerEmail, PageRequest.of(0, pageSize))
                .stream()
                .map(audit -> toResponse(ownerEmail, audit))
                .toList();
    }

    @Transactional
    public AiCommandAuditReverseResponse reverse(String ownerEmail, Long auditId, AiCommandAuditReverseRequest request) {
        AiCommandAudit audit = repository.findByIdAndOwnerEmailIgnoreCase(auditId, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Auditoria de comando da IA nao encontrada."));
        FinanceCommandResult result = readResult(audit.getResultJson());
        AiCommandReversalResponse reversal = resolveReversal(ownerEmail, audit, result);
        if (reversal.status() != AiCommandReversalStatus.DIRECTLY_SUPPORTED) {
            throw new BusinessException("Esta auditoria nao possui reversao automatica segura.");
        }

        String referenceType = reversal.referenceType();
        Long referenceId = reversal.referenceId();
        Object reversalResult;
        String message;
        if ("PAYMENT".equals(referenceType)) {
            PaymentReverseRequest reverseRequest = new PaymentReverseRequest(
                    request == null ? false : Boolean.TRUE.equals(request.deleteLinkedTransaction()),
                    reversalNote(auditId, request == null ? null : request.notes())
            );
            reversalResult = paymentService.reverse(ownerEmail, referenceId, reverseRequest);
            message = "Pagamento revertido a partir da auditoria da IA.";
        } else if ("INSTALLMENT_ANTICIPATION".equals(referenceType)) {
            reversalResult = anticipationService.cancel(ownerEmail, referenceId);
            message = "Antecipacao cancelada a partir da auditoria da IA.";
        } else {
            throw new BusinessException("Tipo de reversao nao suportado.");
        }

        AiCommandAuditReversal savedReversal = saveReversal(audit, referenceType, referenceId, message, reversalResult, request);
        AiCommandAuditReversalEventResponse reversalEvent = AiCommandAuditReversalEventResponse.from(savedReversal);
        return new AiCommandAuditReverseResponse(
                audit.getId(),
                referenceType,
                referenceId,
                message,
                reversalResult,
                reversalEvent,
                toResponse(ownerEmail, audit)
        );
    }

    private AiCommandAuditResponse toResponse(String ownerEmail, AiCommandAudit audit) {
        FinanceCommandResult result = readResult(audit.getResultJson());
        AiCommandAuditReversalEventResponse reversalEvent = reversalRepository
                .findByAuditIdAndOwnerEmailIgnoreCase(audit.getId(), ownerEmail)
                .map(AiCommandAuditReversalEventResponse::from)
                .orElse(null);
        return AiCommandAuditResponse.from(audit, result)
                .withReversal(resolveReversal(ownerEmail, audit, result))
                .withReversalEvent(reversalEvent);
    }

    private FinanceCommandResult readResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(resultJson, FinanceCommandResult.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit));
    }

    private AiCommandReversalResponse resolveReversal(String ownerEmail, AiCommandAudit audit, FinanceCommandResult result) {
        if (audit.getId() != null && reversalRepository.existsByAuditIdAndOwnerEmailIgnoreCase(audit.getId(), ownerEmail)) {
            return new AiCommandReversalResponse(
                    AiCommandReversalStatus.NOT_APPLICABLE,
                    "Acao ja desfeita pela auditoria",
                    "Esta auditoria ja possui um registro de reversao.",
                    null,
                    null
            );
        }
        if (audit.isDryRun() || audit.getStatus() != AiCommandStatus.EXECUTED) {
            return new AiCommandReversalResponse(
                    AiCommandReversalStatus.NOT_APPLICABLE,
                    "Sem desfazimento",
                    "Previas, bloqueios e falhas nao alteram dados financeiros.",
                    null,
                    null
            );
        }
        FinanceCommandType type = audit.getCommandType();
        if (type == FinanceCommandType.REGISTER_PAYMENT || type == FinanceCommandType.REGISTER_INCOME_RECEIPT) {
            Long paymentId = nestedId(result, "payment");
            if (paymentId != null) {
                if (isPaymentAlreadyInactive(ownerEmail, paymentId)) {
                    return new AiCommandReversalResponse(
                            AiCommandReversalStatus.NOT_APPLICABLE,
                            "Pagamento ja revertido ou cancelado",
                            "O pagamento gerado por este comando nao esta mais ativo.",
                            "PAYMENT",
                            paymentId
                    );
                }
                return new AiCommandReversalResponse(
                        AiCommandReversalStatus.DIRECTLY_SUPPORTED,
                        "Reversao de pagamento disponivel",
                        "Este comando gerou um pagamento que pode ser revertido pelo fluxo existente de pagamentos.",
                        "PAYMENT",
                        paymentId
                );
            }
        }
        if (type == FinanceCommandType.ANTICIPATE_INSTALLMENTS) {
            Long anticipationId = nestedId(result, "installmentAnticipation");
            if (anticipationId != null) {
                if (isAnticipationAlreadyCanceled(ownerEmail, anticipationId)) {
                    return new AiCommandReversalResponse(
                            AiCommandReversalStatus.NOT_APPLICABLE,
                            "Antecipacao ja cancelada",
                            "A antecipacao gerada por este comando ja foi cancelada.",
                            "INSTALLMENT_ANTICIPATION",
                            anticipationId
                    );
                }
                return new AiCommandReversalResponse(
                        AiCommandReversalStatus.DIRECTLY_SUPPORTED,
                        "Cancelamento de antecipacao disponivel",
                        "Este comando gerou uma antecipacao que pode ser cancelada pelo fluxo existente de parcelas.",
                        "INSTALLMENT_ANTICIPATION",
                        anticipationId
                );
            }
        }
        if (requiresManualReview(type)) {
            return new AiCommandReversalResponse(
                    AiCommandReversalStatus.MANUAL_REVIEW_REQUIRED,
                    "Revisao manual necessaria",
                    "O comando alterou dados financeiros, mas a auditoria ainda nao guarda informacoes suficientes para desfazer automaticamente com seguranca.",
                    null,
                    null
            );
        }
        return new AiCommandReversalResponse(
                AiCommandReversalStatus.NOT_SUPPORTED,
                "Sem reversao automatica",
                "Este tipo de comando nao possui um fluxo de desfazimento automatizado.",
                null,
                null
        );
    }

    private boolean requiresManualReview(FinanceCommandType type) {
        return type != null && type != FinanceCommandType.CREATE_CATEGORY;
    }

    @SuppressWarnings("unchecked")
    private Long nestedId(FinanceCommandResult result, String key) {
        if (result == null || result.details() == null) {
            return null;
        }
        Object value = result.details().get(key);
        if (value instanceof java.util.Map<?, ?> map) {
            Object id = map.get("id");
            if (id instanceof Number number) {
                return number.longValue();
            }
            if (id instanceof String text && !text.isBlank()) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isPaymentAlreadyInactive(String ownerEmail, Long paymentId) {
        return paymentRepository.findByIdAndOwnerEmail(paymentId, ownerEmail)
                .map(payment -> payment.getStatus() != PaymentStatus.ACTIVE)
                .orElse(false);
    }

    private boolean isAnticipationAlreadyCanceled(String ownerEmail, Long anticipationId) {
        return anticipationRepository.findByIdAndOwnerEmail(anticipationId, ownerEmail)
                .map(anticipation -> anticipation.getStatus() == InstallmentAnticipationStatus.CANCELED)
                .orElse(false);
    }

    private String reversalNote(Long auditId, String notes) {
        String prefix = "Reversao iniciada pela auditoria da IA #" + auditId + ".";
        if (notes == null || notes.isBlank()) {
            return prefix;
        }
        return prefix + " " + notes.trim();
    }

    private AiCommandAuditReversal saveReversal(
            AiCommandAudit audit,
            String referenceType,
            Long referenceId,
            String message,
            Object result,
            AiCommandAuditReverseRequest request
    ) {
        AiCommandAuditReversal reversal = new AiCommandAuditReversal();
        reversal.setOwner(audit.getOwner());
        reversal.setAudit(audit);
        reversal.setReferenceType(referenceType);
        reversal.setReferenceId(referenceId);
        reversal.setMessage(message);
        reversal.setResultJson(writeJsonSafely(result));
        reversal.setNotes(request == null ? null : request.notes());
        return reversalRepository.save(reversal);
    }

    private String writeJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
