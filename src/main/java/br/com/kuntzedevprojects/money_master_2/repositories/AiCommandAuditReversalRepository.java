package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAuditReversal;

public interface AiCommandAuditReversalRepository extends JpaRepository<AiCommandAuditReversal, Long> {

    Optional<AiCommandAuditReversal> findByAuditIdAndOwnerEmailIgnoreCase(Long auditId, String ownerEmail);

    boolean existsByAuditIdAndOwnerEmailIgnoreCase(Long auditId, String ownerEmail);
}
