package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;

public interface AiCommandAuditRepository extends JpaRepository<AiCommandAudit, Long> {

    List<AiCommandAudit> findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(String ownerEmail, Pageable pageable);

    Optional<AiCommandAudit> findByIdAndOwnerEmailIgnoreCase(Long id, String ownerEmail);

    @Modifying
    @Query("update AiCommandAudit a set a.conversation = null where a.conversation.id in :conversationIds")
    int detachConversations(@Param("conversationIds") Collection<Long> conversationIds);
}
