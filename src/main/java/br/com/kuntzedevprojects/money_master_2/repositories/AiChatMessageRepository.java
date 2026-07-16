package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Collection;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.AiChatMessage;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Modifying
    @Query("delete from AiChatMessage m where m.conversation.id in :conversationIds")
    int deleteByConversationIds(@Param("conversationIds") Collection<Long> conversationIds);

    @Modifying
    @Query("delete from AiChatMessage m where m.conversation.id in :conversationIds and m.createdAt < :threshold")
    int deleteByConversationIdsAndCreatedAtBefore(
            @Param("conversationIds") Collection<Long> conversationIds,
            @Param("threshold") Instant threshold
    );
}
