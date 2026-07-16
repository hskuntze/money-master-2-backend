package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialWorkspace;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialWorkspaceType;

public interface FinancialWorkspaceRepository extends JpaRepository<FinancialWorkspace, Long> {

    @Query("""
            select distinct w
            from FinancialWorkspace w
            left join fetch w.owner owner
            left join fetch w.members member
            left join fetch member.user memberUser
            left join fetch member.invitedBy invitedBy
            where w.active = true
              and exists (
                  select 1
                  from WorkspaceMember ownMember
                  where ownMember.workspace = w
                    and lower(ownMember.user.email) = lower(:email)
                    and ownMember.status = br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberStatus.ACTIVE
              )
            order by w.type asc, w.name asc
            """)
    List<FinancialWorkspace> findAccessibleByUserEmail(@Param("email") String email);

    @Query("""
            select distinct w
            from FinancialWorkspace w
            left join fetch w.owner owner
            left join fetch w.members member
            left join fetch member.user memberUser
            left join fetch member.invitedBy invitedBy
            where w.id = :id
              and w.active = true
              and exists (
                  select 1
                  from WorkspaceMember ownMember
                  where ownMember.workspace = w
                    and lower(ownMember.user.email) = lower(:email)
                    and ownMember.status = br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberStatus.ACTIVE
              )
            """)
    Optional<FinancialWorkspace> findAccessibleByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);

    Optional<FinancialWorkspace> findByOwnerEmailIgnoreCaseAndTypeAndActiveTrue(String ownerEmail, FinancialWorkspaceType type);
}
