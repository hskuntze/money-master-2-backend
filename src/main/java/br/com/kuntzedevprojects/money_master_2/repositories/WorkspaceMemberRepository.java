package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceMember;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    @Query("""
            select m
            from WorkspaceMember m
            join fetch m.workspace w
            join fetch m.user u
            left join fetch m.invitedBy invitedBy
            where w.id = :workspaceId
              and lower(u.email) = lower(:email)
            """)
    Optional<WorkspaceMember> findByWorkspaceIdAndUserEmail(@Param("workspaceId") Long workspaceId, @Param("email") String email);

    boolean existsByWorkspaceIdAndUserEmailIgnoreCase(Long workspaceId, String email);
}
