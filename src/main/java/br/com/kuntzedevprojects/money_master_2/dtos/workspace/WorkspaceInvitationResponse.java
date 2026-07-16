package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceInvitation;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceInvitationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberRole;

public record WorkspaceInvitationResponse(
        Long id,
        Long workspaceId,
        String workspaceName,
        String inviteeEmail,
        WorkspaceMemberRole role,
        WorkspaceInvitationStatus status,
        String token,
        Long invitedById,
        String invitedByName,
        Instant expiresAt,
        Instant acceptedAt,
        Instant canceledAt,
        Instant createdAt
) {
    public static WorkspaceInvitationResponse from(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getInviteeEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getToken(),
                invitation.getInvitedBy().getId(),
                invitation.getInvitedBy().getName(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCanceledAt(),
                invitation.getCreatedAt()
        );
    }
}
