package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.config.properties.JwtProperties;
import br.com.kuntzedevprojects.money_master_2.entities.RefreshToken;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            TokenHashService tokenHashService,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = tokenHashService.generateRawToken();
        String tokenHash = tokenHashService.sha256(rawToken);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setIssuedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenDays() * 24 * 60 * 60));

        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public User consume(String rawToken) {
        String tokenHash = tokenHashService.sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Refresh token inválido."));

        if (token.isRevoked() || token.isExpired()) {
            throw new BusinessException("Refresh token expirado ou revogado.");
        }

        token.setRevokedAt(Instant.now());
        return token.getUser();
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeActiveTokensByUserId(user.getId(), Instant.now());
    }
}
