package br.com.kuntzedevprojects.money_master_2.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.EmailConfirmationToken;

public interface EmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, Long> {

    Optional<EmailConfirmationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update EmailConfirmationToken t set t.usedAt = :usedAt where t.user.id = :userId and t.usedAt is null")
    int markOpenTokensAsUsedByUserId(@Param("userId") Long userId, @Param("usedAt") Instant usedAt);
}
