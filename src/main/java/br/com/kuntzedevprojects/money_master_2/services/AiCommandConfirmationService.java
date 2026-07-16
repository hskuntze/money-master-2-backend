package br.com.kuntzedevprojects.money_master_2.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.config.properties.FinanceAiProperties;
import br.com.kuntzedevprojects.money_master_2.config.properties.JwtProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandItem;
import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandConfirmation;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandConfirmationRepository;

@Service
public class AiCommandConfirmationService {

    private final AiCommandConfirmationRepository confirmationRepository;
    private final CurrentUserService currentUserService;
    private final TokenHashService tokenHashService;
    private final ObjectMapper objectMapper;
    private final FinanceAiProperties financeAiProperties;
    private final JwtProperties jwtProperties;

    public AiCommandConfirmationService(
            AiCommandConfirmationRepository confirmationRepository,
            CurrentUserService currentUserService,
            TokenHashService tokenHashService,
            ObjectMapper objectMapper,
            FinanceAiProperties financeAiProperties,
            JwtProperties jwtProperties
    ) {
        this.confirmationRepository = confirmationRepository;
        this.currentUserService = currentUserService;
        this.tokenHashService = tokenHashService;
        this.objectMapper = objectMapper;
        this.financeAiProperties = financeAiProperties;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public IssuedConfirmation create(String ownerEmail, List<FinanceCommandItem> commands, String reason) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        AiChatConversation conversation = FinanceAiConversationContext.get();
        String commandJson = canonicalCommandsJson(commands);
        String commandHash = tokenHashService.sha256(commandJson);
        Instant expiresAt = Instant.now().plusSeconds(Math.max(1, financeAiProperties.getConfirmationTokenMinutes()) * 60);
        String token = signedToken(owner, conversation, commandHash, expiresAt);

        AiCommandConfirmation confirmation = new AiCommandConfirmation();
        confirmation.setOwner(owner);
        confirmation.setConversation(conversation);
        confirmation.setTokenHash(tokenHashService.sha256(token));
        confirmation.setCommandHash(commandHash);
        confirmation.setCommandJson(commandJson);
        confirmation.setReason(trim(reason, 1000));
        confirmation.setExpiresAt(expiresAt);
        confirmationRepository.save(confirmation);

        return new IssuedConfirmation(token, expiresAt);
    }

    @Transactional
    public void validateAndConsume(String ownerEmail, List<FinanceCommandItem> commands, String confirmationToken) {
        if (confirmationToken == null || confirmationToken.isBlank()) {
            throw new BusinessException("Para executar este comando financeiro, gere uma previa e informe o token de confirmacao retornado pelo backend.");
        }
        SignedTokenPayload payload = verifySignedToken(confirmationToken.trim());
        String expectedCommandHash = tokenHashService.sha256(canonicalCommandsJson(commands));
        if (!Objects.equals(payload.commandHash(), expectedCommandHash)) {
            throw new BusinessException("A confirmacao nao corresponde aos comandos enviados. Gere uma nova previa antes de executar.");
        }

        AiCommandConfirmation confirmation = confirmationRepository
                .findForValidation(tokenHashService.sha256(confirmationToken.trim()), ownerEmail)
                .orElseThrow(() -> new BusinessException("Confirmacao invalida ou pertencente a outro usuario."));

        if (confirmation.isUsed()) {
            throw new BusinessException("Esta confirmacao ja foi utilizada. Gere uma nova previa para repetir a operacao.");
        }
        if (confirmation.isExpired() || payload.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException("A confirmacao expirou. Gere uma nova previa antes de executar.");
        }
        if (!Objects.equals(confirmation.getCommandHash(), expectedCommandHash)) {
            throw new BusinessException("A confirmacao registrada nao corresponde aos comandos enviados.");
        }
        if (!Objects.equals(confirmation.getOwner().getId(), payload.ownerId())) {
            throw new BusinessException("Confirmacao invalida para este usuario.");
        }

        AiChatConversation currentConversation = FinanceAiConversationContext.get();
        AiChatConversation tokenConversation = confirmation.getConversation();
        if (tokenConversation != null) {
            if (currentConversation == null || !Objects.equals(tokenConversation.getConversationKey(), currentConversation.getConversationKey())) {
                throw new BusinessException("A confirmacao deve ser usada na mesma conversa em que a previa foi gerada.");
            }
            if (!Objects.equals(payload.conversationKey(), tokenConversation.getConversationKey())) {
                throw new BusinessException("Confirmacao invalida para esta conversa.");
            }
        }

        confirmation.setUsedAt(Instant.now());
        confirmationRepository.save(confirmation);
    }

    public String canonicalCommandsJson(List<FinanceCommandItem> commands) {
        try {
            return objectMapper.writeValueAsString(commands == null ? List.of() : commands);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Nao foi possivel preparar a confirmacao dos comandos financeiros.");
        }
    }

    private String signedToken(User owner, AiChatConversation conversation, String commandHash, Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nonce", tokenHashService.generateRawToken());
        payload.put("ownerId", owner.getId());
        payload.put("conversationKey", conversation == null ? null : conversation.getConversationKey());
        payload.put("commandHash", commandHash);
        payload.put("expiresAt", expiresAt.toString());
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            return encodedPayload + "." + sign(encodedPayload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Nao foi possivel gerar o token de confirmacao.");
        }
    }

    private SignedTokenPayload verifySignedToken(String token) {
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException("Token de confirmacao invalido.");
        }
        String expectedSignature = sign(parts[0]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("Token de confirmacao invalido.");
        }
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
            return new SignedTokenPayload(
                    Long.valueOf(String.valueOf(payload.get("ownerId"))),
                    payload.get("conversationKey") == null ? null : String.valueOf(payload.get("conversationKey")),
                    String.valueOf(payload.get("commandHash")),
                    Instant.parse(String.valueOf(payload.get("expiresAt")))
            );
        } catch (Exception ex) {
            throw new BusinessException("Token de confirmacao invalido.");
        }
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException("Nao foi possivel assinar o token de confirmacao.");
        }
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    public record IssuedConfirmation(String token, Instant expiresAt) {
    }

    private record SignedTokenPayload(Long ownerId, String conversationKey, String commandHash, Instant expiresAt) {
    }
}
