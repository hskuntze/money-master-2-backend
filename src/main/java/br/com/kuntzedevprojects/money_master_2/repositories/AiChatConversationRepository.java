package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;

public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, Long> {

    Optional<AiChatConversation> findByConversationKeyAndOwnerEmailIgnoreCase(String conversationKey, String ownerEmail);

    @Query("""
            select c.id
            from AiChatConversation c
            where lower(c.owner.email) = lower(:ownerEmail)
            """)
    List<Long> findIdsByOwnerEmail(@Param("ownerEmail") String ownerEmail);
}
