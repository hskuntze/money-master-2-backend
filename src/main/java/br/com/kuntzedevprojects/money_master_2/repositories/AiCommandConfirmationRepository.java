package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.AiCommandConfirmation;
import jakarta.persistence.LockModeType;

public interface AiCommandConfirmationRepository extends JpaRepository<AiCommandConfirmation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from AiCommandConfirmation c
            join fetch c.owner o
            left join fetch c.conversation conversation
            where c.tokenHash = :tokenHash
              and lower(o.email) = lower(:ownerEmail)
            """)
    Optional<AiCommandConfirmation> findForValidation(
            @Param("tokenHash") String tokenHash,
            @Param("ownerEmail") String ownerEmail
    );

    @Modifying
    @Query("update AiCommandConfirmation c set c.conversation = null where c.conversation.id in :conversationIds")
    int detachConversations(@Param("conversationIds") Collection<Long> conversationIds);
}
