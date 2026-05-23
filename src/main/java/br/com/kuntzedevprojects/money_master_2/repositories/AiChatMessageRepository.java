package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.AiChatMessage;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
