package br.com.kuntzedevprojects.money_master_2.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public CategoryService(CategoryRepository categoryRepository, CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAvailable(String ownerEmail, TransactionType type) {
        return categoryRepository.findAvailableForUser(ownerEmail, type)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(String ownerEmail, Long id) {
        return CategoryResponse.from(findAvailableCategory(ownerEmail, id));
    }

    @Transactional
    public CategoryResponse create(String ownerEmail, CategoryCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        String name = normalizeRequired(request.name(), "O nome da categoria é obrigatório.");

        ensureUserCategoryDoesNotExist(ownerEmail, name, request.type(), null);

        Category category = new Category();
        category.setOwner(owner);
        category.setName(name);
        category.setType(request.type());
        category.setIcon(normalizeNullable(request.icon()));
        category.setColor(normalizeNullable(request.color()));
        category.setSystemDefault(false);
        category.setActive(request.active() == null || request.active());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse createSystem(CategoryCreateRequest request) {
        String name = normalizeRequired(request.name(), "O nome da categoria é obrigatório.");
        ensureSystemCategoryDoesNotExist(name, request.type(), null);

        Category category = new Category();
        category.setOwner(null);
        category.setName(name);
        category.setType(request.type());
        category.setIcon(normalizeNullable(request.icon()));
        category.setColor(normalizeNullable(request.color()));
        category.setSystemDefault(true);
        category.setActive(request.active() == null || request.active());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateSystem(Long id, CategoryUpdateRequest request) {
        Category category = findSystemCategory(id);

        String newName = request.name() != null && !request.name().isBlank() ? request.name().trim() : category.getName();
        TransactionType newType = request.type() == null ? category.getType() : request.type();
        ensureSystemCategoryDoesNotExist(newName, newType, id);

        category.setName(newName);
        category.setType(newType);

        if (request.icon() != null) {
            category.setIcon(normalizeNullable(request.icon()));
        }
        if (request.color() != null) {
            category.setColor(normalizeNullable(request.color()));
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return CategoryResponse.from(category);
    }

    @Transactional
    public void deactivateSystem(Long id) {
        Category category = findSystemCategory(id);
        category.setActive(false);
    }

    @Transactional
    public CategoryResponse update(String ownerEmail, Long id, CategoryUpdateRequest request) {
        Category category = findOwnedUserCategory(ownerEmail, id);

        String newName = request.name() != null && !request.name().isBlank() ? request.name().trim() : category.getName();
        TransactionType newType = request.type() == null ? category.getType() : request.type();
        ensureUserCategoryDoesNotExist(ownerEmail, newName, newType, id);

        category.setName(newName);
        category.setType(newType);

        if (request.icon() != null) {
            category.setIcon(normalizeNullable(request.icon()));
        }
        if (request.color() != null) {
            category.setColor(normalizeNullable(request.color()));
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return CategoryResponse.from(category);
    }

    @Transactional
    public void deactivate(String ownerEmail, Long id) {
        Category category = findOwnedUserCategory(ownerEmail, id);
        category.setActive(false);
    }

    @Transactional
    public Category resolveForAi(String ownerEmail, Long categoryId, String categoryName, TransactionType type) {
        if (categoryId != null) {
            Category category = findAvailableCategory(ownerEmail, categoryId);
            if (category.getType() != type) {
                throw new BusinessException("A categoria informada não pertence ao tipo " + type + ".");
            }
            return category;
        }

        String normalizedName = normalizeCategoryName(categoryName, type);
        List<Category> existing = categoryRepository.findAvailableByNameAndType(ownerEmail, normalizedName, type);
        if (!existing.isEmpty()) {
            return existing.stream()
                    .filter(Category::isActive)
                    .min(Comparator.comparing(Category::isSystemDefault).reversed())
                    .orElse(existing.get(0));
        }

        User owner = currentUserService.findUserByEmail(ownerEmail);
        Category category = new Category();
        category.setOwner(owner);
        category.setName(normalizedName);
        category.setType(type);
        category.setSystemDefault(false);
        category.setActive(true);
        category.setIcon(defaultIcon(type));
        category.setColor(defaultColor(type));
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category findAvailableCategory(String ownerEmail, Long id) {
        return categoryRepository.findAvailableById(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
    }

    private Category findSystemCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
        if (!category.isSystemDefault()) {
            throw new BusinessException("Esta categoria não é uma categoria padrão do sistema.");
        }
        return category;
    }

    private Category findOwnedUserCategory(String ownerEmail, Long id) {
        Category category = categoryRepository.findAvailableById(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
        if (category.isSystemDefault()) {
            throw new BusinessException("Categorias padrão do sistema não podem ser alteradas por este endpoint.");
        }
        if (category.getOwner() == null || !category.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new BusinessException("Esta categoria não pertence ao usuário autenticado.");
        }
        return category;
    }

    private void ensureSystemCategoryDoesNotExist(String name, TransactionType type, Long ignoreId) {
        categoryRepository.findSystemDefaultByNameAndType(name, type)
                .filter(existing -> ignoreId == null || !existing.getId().equals(ignoreId))
                .ifPresent(existing -> {
                    throw new BusinessException("Já existe uma categoria padrão do sistema com este nome e tipo.");
                });
    }

    private void ensureUserCategoryDoesNotExist(String ownerEmail, String name, TransactionType type, Long ignoreId) {
        categoryRepository.findUserCategoryByNameAndType(ownerEmail, name, type)
                .filter(existing -> ignoreId == null || !existing.getId().equals(ignoreId))
                .ifPresent(existing -> {
                    throw new BusinessException("Já existe uma categoria personalizada com este nome e tipo.");
                });
    }

    private String normalizeCategoryName(String categoryName, TransactionType type) {
        if (categoryName == null || categoryName.isBlank()) {
            return switch (type) {
                case INCOME -> "Receitas";
                case EXPENSE -> "Outros";
                case TRANSFER -> "Transferências";
            };
        }
        return categoryName.trim();
    }

    private String defaultIcon(TransactionType type) {
        return switch (type) {
            case INCOME -> "trending-up";
            case EXPENSE -> "shopping-cart";
            case TRANSFER -> "repeat";
        };
    }

    private String defaultColor(TransactionType type) {
        return switch (type) {
            case INCOME -> "#16a34a";
            case EXPENSE -> "#dc2626";
            case TRANSFER -> "#2563eb";
        };
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
