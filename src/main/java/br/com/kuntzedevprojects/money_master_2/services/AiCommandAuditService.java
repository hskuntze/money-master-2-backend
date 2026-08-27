package br.com.kuntzedevprojects.money_master_2.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandReversalResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandResult;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandReversalStatus;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;

@Service
public class AiCommandAuditService {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final AiCommandAuditRepository repository;
    private final ObjectMapper objectMapper;

    public AiCommandAuditService(AiCommandAuditRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AiCommandAuditResponse> listRecent(String ownerEmail, Integer limit) {
        int pageSize = normalizeLimit(limit);
        return repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(ownerEmail, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AiCommandAuditResponse toResponse(AiCommandAudit audit) {
        FinanceCommandResult result = readResult(audit.getResultJson());
        return AiCommandAuditResponse.from(audit, result)
                .withReversal(resolveReversal(audit, result));
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

    private AiCommandReversalResponse resolveReversal(AiCommandAudit audit, FinanceCommandResult result) {
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
}
