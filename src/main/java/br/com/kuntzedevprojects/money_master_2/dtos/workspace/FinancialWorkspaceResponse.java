package br.com.kuntzedevprojects.money_master_2.dtos.workspace;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialWorkspace;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialWorkspaceType;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberStatus;

public record FinancialWorkspaceResponse(
        Long id,
        String name,
        FinancialWorkspaceType type,
        boolean active,
        Long ownerId,
        String ownerName,
        List<WorkspaceMemberResponse> members,
        Instant createdAt,
        Instant updatedAt
) {
    public static FinancialWorkspaceResponse from(FinancialWorkspace workspace) {
        List<WorkspaceMemberResponse> members = workspace.getMembers() == null
                ? List.of()
                : workspace.getMembers().stream()
                        .filter(member -> member.getStatus() == WorkspaceMemberStatus.ACTIVE)
                        .sorted(Comparator.comparing(member -> member.getRole().ordinal()))
                        .map(WorkspaceMemberResponse::from)
                        .toList();
        return new FinancialWorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getType(),
                workspace.isActive(),
                workspace.getOwner().getId(),
                workspace.getOwner().getName(),
                members,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}
