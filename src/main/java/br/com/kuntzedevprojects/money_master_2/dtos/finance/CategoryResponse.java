package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        String icon,
        String color,
        boolean systemDefault,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getIcon(),
                category.getColor(),
                category.isSystemDefault(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
