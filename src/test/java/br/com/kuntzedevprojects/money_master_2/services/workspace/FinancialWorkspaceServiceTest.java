package br.com.kuntzedevprojects.money_master_2.services.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceInviteRequest;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialWorkspace;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceMember;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialWorkspaceType;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberRole;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberStatus;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialWorkspaceRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.WorkspaceInvitationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.WorkspaceMemberRepository;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;

class FinancialWorkspaceServiceTest {

    private final FinancialWorkspaceRepository workspaceRepository = mock(FinancialWorkspaceRepository.class);
    private final WorkspaceMemberRepository memberRepository = mock(WorkspaceMemberRepository.class);
    private final WorkspaceInvitationRepository invitationRepository = mock(WorkspaceInvitationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final FinancialWorkspaceService service = new FinancialWorkspaceService(
            workspaceRepository,
            memberRepository,
            invitationRepository,
            userRepository,
            currentUserService
    );

    @Test
    void shouldReuseExistingPersonalWorkspace() {
        User owner = owner();
        FinancialWorkspace existing = personalWorkspace(owner);
        when(workspaceRepository.findByOwnerEmailIgnoreCaseAndTypeAndActiveTrue(
                "ana@example.com",
                FinancialWorkspaceType.PERSONAL
        )).thenReturn(Optional.of(existing));

        FinancialWorkspace response = service.createPersonalWorkspaceFor(owner);

        assertThat(response).isSameAs(existing);
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void shouldCreatePersonalWorkspaceWithOwnerMembership() {
        User owner = owner();
        when(workspaceRepository.findByOwnerEmailIgnoreCaseAndTypeAndActiveTrue(
                "ana@example.com",
                FinancialWorkspaceType.PERSONAL
        )).thenReturn(Optional.empty());
        when(workspaceRepository.save(any(FinancialWorkspace.class))).thenAnswer(invocation -> {
            FinancialWorkspace workspace = invocation.getArgument(0);
            workspace.setId(20L);
            workspace.getMembers().getFirst().setId(30L);
            return workspace;
        });

        FinancialWorkspace response = service.createPersonalWorkspaceFor(owner);

        assertThat(response.getType()).isEqualTo(FinancialWorkspaceType.PERSONAL);
        assertThat(response.getMembers()).hasSize(1);
        assertThat(response.getMembers().getFirst().getRole()).isEqualTo(WorkspaceMemberRole.OWNER);
        assertThat(response.getMembers().getFirst().getStatus()).isEqualTo(WorkspaceMemberStatus.ACTIVE);
    }

    @Test
    void shouldRejectInviteFromPersonalWorkspace() {
        User owner = owner();
        FinancialWorkspace workspace = personalWorkspace(owner);
        when(workspaceRepository.findAccessibleByIdAndUserEmail(10L, "ana@example.com")).thenReturn(Optional.of(workspace));

        assertThatThrownBy(() -> service.invite(
                "ana@example.com",
                10L,
                new WorkspaceInviteRequest("bia@example.com", WorkspaceMemberRole.MEMBER)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("espaco familiar");
    }

    private User owner() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Ana Silva");
        owner.setEmail("ana@example.com");
        return owner;
    }

    private FinancialWorkspace personalWorkspace(User owner) {
        FinancialWorkspace workspace = new FinancialWorkspace();
        workspace.setId(10L);
        workspace.setOwner(owner);
        workspace.setName("Espaco pessoal");
        workspace.setType(FinancialWorkspaceType.PERSONAL);
        workspace.setActive(true);

        WorkspaceMember member = new WorkspaceMember();
        member.setId(11L);
        member.setUser(owner);
        member.setRole(WorkspaceMemberRole.OWNER);
        member.setStatus(WorkspaceMemberStatus.ACTIVE);
        workspace.addMember(member);
        return workspace;
    }
}
