package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.entities.EmailConfirmationToken;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.EmailConfirmationTokenRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;

@Service
public class EmailConfirmationService {

    private static final long TOKEN_EXPIRATION_HOURS = 24;

    private final EmailConfirmationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TokenHashService tokenHashService;
    private final EmailService emailService;

    public EmailConfirmationService(
            EmailConfirmationTokenRepository tokenRepository,
            UserRepository userRepository,
            TokenHashService tokenHashService,
            EmailService emailService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenHashService = tokenHashService;
        this.emailService = emailService;
    }

    @Transactional
    public void createAndSend(User user) {
        tokenRepository.markOpenTokensAsUsedByUserId(user.getId(), Instant.now());

        String rawToken = tokenHashService.generateRawToken();
        EmailConfirmationToken token = new EmailConfirmationToken();
        token.setUser(user);
        token.setTokenHash(tokenHashService.sha256(rawToken));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(TOKEN_EXPIRATION_HOURS * 60 * 60));
        tokenRepository.save(token);

        emailService.sendConfirmationEmail(user, rawToken);
    }

    @Transactional
    public void confirm(String rawToken) {
        EmailConfirmationToken token = tokenRepository.findByTokenHash(tokenHashService.sha256(rawToken))
                .orElseThrow(() -> new BusinessException("Token de confirmação inválido."));

        if (token.isUsed()) {
            throw new BusinessException("Token de confirmação já utilizado.");
        }
        if (token.isExpired()) {
            throw new BusinessException("Token de confirmação expirado.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        token.setUsedAt(Instant.now());
        userRepository.save(user);
    }
}
