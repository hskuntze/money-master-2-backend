package br.com.kuntzedevprojects.money_master_2.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.reference.FinancialReferenceRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.reference.FinancialReferenceResponse;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialReference;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialReferenceRepository;

@Service
public class FinancialReferenceService {

    private final FinancialReferenceRepository referenceRepository;
    private final CurrentUserService currentUserService;

    public FinancialReferenceService(FinancialReferenceRepository referenceRepository, CurrentUserService currentUserService) {
        this.referenceRepository = referenceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<FinancialReferenceResponse> list(String ownerEmail, boolean activeOnly) {
        return referenceRepository.findAvailableForOwner(ownerEmail, activeOnly)
                .stream()
                .map(FinancialReferenceResponse::from)
                .toList();
    }

    @Transactional
    public FinancialReferenceResponse create(String ownerEmail, FinancialReferenceRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        FinancialReference reference = new FinancialReference();
        reference.setOwner(owner);
        apply(reference, request);
        return FinancialReferenceResponse.from(referenceRepository.save(reference));
    }

    @Transactional
    public FinancialReferenceResponse update(String ownerEmail, Long id, FinancialReferenceRequest request) {
        FinancialReference reference = referenceRepository.findByIdAvailableForOwner(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Referência financeira não encontrada."));
        apply(reference, request);
        return FinancialReferenceResponse.from(reference);
    }

    @Transactional(readOnly = true)
    public String buildPromptContext(String ownerEmail) {
        List<FinancialReference> references = referenceRepository.findAvailableForOwner(ownerEmail, true).stream().limit(8).toList();
        if (references.isEmpty()) {
            return "Não há referências financeiras cadastradas para fundamentar respostas além do conhecimento geral do modelo.";
        }
        return references.stream()
                .map(reference -> "- " + reference.getTitle() + " (" + reference.getType() + ")"
                        + (reference.getSource() == null ? "" : ", fonte: " + reference.getSource())
                        + (reference.getUrl() == null ? "" : ", URL: " + reference.getUrl()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private void apply(FinancialReference reference, FinancialReferenceRequest request) {
        reference.setTitle(normalize(request.title(), 180));
        reference.setType(request.type());
        reference.setUrl(normalize(request.url(), 1000));
        reference.setDescription(normalize(request.description(), 2000));
        reference.setSource(normalize(request.source(), 180));
        reference.setActive(request.active() == null || request.active());
    }

    private String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
