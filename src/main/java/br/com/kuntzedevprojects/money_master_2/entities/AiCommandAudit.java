package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "tb_ai_command_audit",
        indexes = {
                @Index(name = "idx_ai_command_audit_owner", columnList = "owner_id,created_at"),
                @Index(name = "idx_ai_command_audit_conversation", columnList = "conversation_id,created_at")
        }
)
public class AiCommandAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private AiChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private FinanceCommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiCommandStatus status;

    @Column(nullable = false)
    private boolean dryRun;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String commandJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public AiChatConversation getConversation() {
        return conversation;
    }

    public void setConversation(AiChatConversation conversation) {
        this.conversation = conversation;
    }

    public FinanceCommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(FinanceCommandType commandType) {
        this.commandType = commandType;
    }

    public AiCommandStatus getStatus() {
        return status;
    }

    public void setStatus(AiCommandStatus status) {
        this.status = status;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getCommandJson() {
        return commandJson;
    }

    public void setCommandJson(String commandJson) {
        this.commandJson = commandJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
