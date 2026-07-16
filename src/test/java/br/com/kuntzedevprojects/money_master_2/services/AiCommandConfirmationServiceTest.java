package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.config.properties.FinanceAiProperties;
import br.com.kuntzedevprojects.money_master_2.config.properties.JwtProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandItem;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandConfirmation;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandConfirmationRepository;

class AiCommandConfirmationServiceTest {

    private final AiCommandConfirmationRepository confirmationRepository = mock(AiCommandConfirmationRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final TokenHashService tokenHashService = new TokenHashService();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FinanceAiProperties financeAiProperties = new FinanceAiProperties();
    private final JwtProperties jwtProperties = new JwtProperties();
    private final AiCommandConfirmationService service = new AiCommandConfirmationService(
            confirmationRepository,
            currentUserService,
            tokenHashService,
            objectMapper,
            financeAiProperties,
            jwtProperties
    );

    @Test
    void shouldCreateAndConsumeConfirmationOnceForSameCommandBatch() {
        User owner = owner();
        AiCommandConfirmation[] saved = new AiCommandConfirmation[1];
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(confirmationRepository.save(any(AiCommandConfirmation.class))).thenAnswer(invocation -> {
            saved[0] = invocation.getArgument(0);
            return saved[0];
        });
        when(confirmationRepository.findForValidation(anyString(), anyString())).thenAnswer(invocation -> Optional.of(saved[0]));
        List<FinanceCommandItem> commands = List.of(command("Mercado"));

        AiCommandConfirmationService.IssuedConfirmation issued = service.create("ana@example.com", commands, "teste");
        service.validateAndConsume("ana@example.com", commands, issued.token());

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(saved[0].getUsedAt()).isNotNull();
        assertThatThrownBy(() -> service.validateAndConsume("ana@example.com", commands, issued.token()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("utilizada");
    }

    @Test
    void shouldRejectConfirmationWhenCommandBatchChangesAfterPreview() {
        User owner = owner();
        AiCommandConfirmation[] saved = new AiCommandConfirmation[1];
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(confirmationRepository.save(any(AiCommandConfirmation.class))).thenAnswer(invocation -> {
            saved[0] = invocation.getArgument(0);
            return saved[0];
        });
        List<FinanceCommandItem> previewCommands = List.of(command("Mercado"));
        List<FinanceCommandItem> changedCommands = List.of(command("Farmacia"));

        AiCommandConfirmationService.IssuedConfirmation issued = service.create("ana@example.com", previewCommands, "teste");

        assertThatThrownBy(() -> service.validateAndConsume("ana@example.com", changedCommands, issued.token()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao corresponde");
    }

    private FinanceCommandItem command(String description) {
        try {
            return objectMapper.readValue("""
                    {
                      "type": "REGISTER_TRANSACTION",
                      "transactionType": "EXPENSE",
                      "amount": 45.90,
                      "description": "%s",
                      "occurredOn": "2026-07-16",
                      "categoryName": "Alimentacao",
                      "originalMessage": "gastei no mercado"
                    }
                    """.formatted(description), FinanceCommandItem.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private User owner() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana");
        user.setEmail("ana@example.com");
        return user;
    }
}
