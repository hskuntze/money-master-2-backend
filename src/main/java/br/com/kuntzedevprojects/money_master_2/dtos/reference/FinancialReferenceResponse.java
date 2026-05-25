package br.com.kuntzedevprojects.money_master_2.dtos.reference;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialReference;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialReferenceType;

public record FinancialReferenceResponse(
        Long id,
        String title,
        FinancialReferenceType type,
        String url,
        String description,
        String source,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static FinancialReferenceResponse from(FinancialReference reference) {
        return new FinancialReferenceResponse(
                reference.getId(),
                reference.getTitle(),
                reference.getType(),
                reference.getUrl(),
                reference.getDescription(),
                reference.getSource(),
                reference.isActive(),
                reference.getCreatedAt(),
                reference.getUpdatedAt()
        );
    }
}
