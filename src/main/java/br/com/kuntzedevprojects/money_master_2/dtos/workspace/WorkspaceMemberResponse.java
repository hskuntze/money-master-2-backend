package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceMember;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberRole;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberStatus;

public record WorkspaceMemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        WorkspaceMemberRole role,
        WorkspaceMemberStatus status,
        Long invitedById,
        String invitedByName,
        Instant joinedAt,
        Instant removedAt
) {
    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getStatus(),
                member.getInvitedBy() == null ? null : member.getInvitedBy().getId(),
                member.getInvitedBy() == null ? null : member.getInvitedBy().getName(),
                member.getJoinedAt(),
                member.getRemovedAt()
        );
    }
}
