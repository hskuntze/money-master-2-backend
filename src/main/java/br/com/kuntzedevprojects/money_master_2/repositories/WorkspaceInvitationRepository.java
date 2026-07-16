package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceInvitation;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceInvitationStatus;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    @Query("""
            select invitation
            from WorkspaceInvitation invitation
            join fetch invitation.workspace workspace
            join fetch invitation.invitedBy invitedBy
            where workspace.id = :workspaceId
            order by invitation.createdAt desc
            """)
    List<WorkspaceInvitation> findByWorkspaceIdWithRelations(@Param("workspaceId") Long workspaceId);

    @Query("""
            select invitation
            from WorkspaceInvitation invitation
            join fetch invitation.workspace workspace
            join fetch invitation.invitedBy invitedBy
            left join fetch workspace.members member
            left join fetch member.user memberUser
            where invitation.token = :token
            """)
    Optional<WorkspaceInvitation> findByTokenWithRelations(@Param("token") String token);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndInviteeEmailIgnoreCaseAndStatus(Long workspaceId, String email, WorkspaceInvitationStatus status);
}
