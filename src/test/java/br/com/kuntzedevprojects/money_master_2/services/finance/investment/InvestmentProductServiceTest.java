package br.com.kuntzedevprojects.money_master_2.services.finance.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductResponse;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentMovement;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.InvestmentMovementRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InvestmentProductRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;

class InvestmentProductServiceTest {

    private final InvestmentProductRepository productRepository = mock(InvestmentProductRepository.class);
    private final InvestmentMovementRepository movementRepository = mock(InvestmentMovementRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final InvestmentProductService service = new InvestmentProductService(
            productRepository,
            movementRepository,
            currentUserService,
            accountService
    );

    @Test
    void shouldCreateProductWithInitialBalanceSeparatedFromYield() {
        User owner = owner();
        InvestmentProduct saved = product(owner);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(productRepository.existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(owner.getEmail(), "Capitalização")).thenReturn(false);
        when(productRepository.save(any(InvestmentProduct.class))).thenReturn(saved);
        when(movementRepository.save(any(InvestmentMovement.class))).thenAnswer(invocation -> {
            InvestmentMovement movement = invocation.getArgument(0);
            movement.setId(movement.getType() == InvestmentMovementType.INITIAL_BALANCE ? 100L : 101L);
            return movement;
        });
        when(movementRepository.calculateCurrentAmountUntil(saved.getId(), null)).thenReturn(new BigDecimal("1000.00"));
        when(movementRepository.calculateTotalContributed(saved.getId())).thenReturn(new BigDecimal("950.00"));
        when(movementRepository.calculateTotalWithdrawn(saved.getId())).thenReturn(BigDecimal.ZERO);
        when(movementRepository.calculateTotalYield(saved.getId())).thenReturn(new BigDecimal("50.00"));

        InvestmentProductResponse response = service.create(owner.getEmail(), new InvestmentProductCreateRequest(
                "Capitalização",
                "Capitalização",
                "Banco",
                null,
                "Resgate conforme contrato",
                new BigDecimal("1000.00"),
                new BigDecimal("50.00"),
                LocalDate.of(2026, 8, 1),
                true,
                "Produto com resgate futuro."
        ));

        assertThat(response.currentAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.totalContributed()).isEqualByComparingTo("950.00");
        assertThat(response.totalYield()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldRejectWithdrawalAboveCurrentAmount() {
        User owner = owner();
        InvestmentProduct product = product(owner);
        when(productRepository.findByIdAndOwnerEmailWithAccount(product.getId(), owner.getEmail())).thenReturn(Optional.of(product));
        when(movementRepository.calculateCurrentAmountUntil(product.getId(), null)).thenReturn(new BigDecimal("200.00"));

        assertThatThrownBy(() -> service.withdraw(owner.getEmail(), product.getId(), new InvestmentMovementRequest(
                new BigDecimal("250.00"),
                LocalDate.of(2026, 8, 10),
                "Resgate",
                null,
                null
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser maior que o saldo atual");
    }

    private User owner() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Ana");
        owner.setEmail("ana@example.com");
        return owner;
    }

    private InvestmentProduct product(User owner) {
        InvestmentProduct product = new InvestmentProduct();
        product.setId(10L);
        product.setOwner(owner);
        product.setName("Capitalização");
        product.setTypeName("Capitalização");
        product.setInstitutionName("Banco");
        product.setLiquidity("Resgate conforme contrato");
        product.setActive(true);
        return product;
    }
}
