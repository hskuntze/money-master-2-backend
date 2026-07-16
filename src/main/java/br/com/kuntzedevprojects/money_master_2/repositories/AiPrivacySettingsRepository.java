package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.AiPrivacySettings;

public interface AiPrivacySettingsRepository extends JpaRepository<AiPrivacySettings, Long> {

    @EntityGraph(attributePaths = "owner")
    Optional<AiPrivacySettings> findByOwnerEmailIgnoreCase(String ownerEmail);
}
