package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.AiChatConversation;

public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, Long> {

    Optional<AiChatConversation> findByConversationKeyAndOwnerEmailIgnoreCase(String conversationKey, String ownerEmail);
}
