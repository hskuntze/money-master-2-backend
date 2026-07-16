package br.com.kuntzedevprojects.money_master_2.services.workspace;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.workspace.FinancialWorkspaceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceInvitationResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceInviteRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceMemberRoleUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialWorkspace;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceInvitation;
import br.com.kuntzedevprojects.money_master_2.entities.WorkspaceMember;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialWorkspaceType;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceInvitationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberRole;
import br.com.kuntzedevprojects.money_master_2.enums.WorkspaceMemberStatus;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialWorkspaceRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.WorkspaceInvitationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.WorkspaceMemberRepository;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;

@Service
public class FinancialWorkspaceService {

    private final FinancialWorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SecureRandom secureRandom = new SecureRandom();

    public FinancialWorkspaceService(
            FinancialWorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            WorkspaceInvitationRepository invitationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<FinancialWorkspaceResponse> list(String ownerEmail) {
        return workspaceRepository.findAccessibleByUserEmail(ownerEmail)
                .stream()
                .sorted(Comparator.comparing(FinancialWorkspace::getType).thenComparing(FinancialWorkspace::getName))
                .map(FinancialWorkspaceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinancialWorkspaceResponse get(String ownerEmail, Long id) {
        return FinancialWorkspaceResponse.from(findAccessibleWorkspace(ownerEmail, id));
    }

    @Transactional
    public FinancialWorkspaceResponse create(String ownerEmail, WorkspaceCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        FinancialWorkspace workspace = new FinancialWorkspace();
        workspace.setOwner(owner);
        workspace.setName(normalizeRequired(request.name(), "O nome do espaco financeiro e obrigatorio."));
        workspace.setType(request.type() == null ? FinancialWorkspaceType.HOUSEHOLD : request.type());
        workspace.setActive(true);

        WorkspaceMember ownerMember = new WorkspaceMember();
        ownerMember.setUser(owner);
        ownerMember.setRole(WorkspaceMemberRole.OWNER);
        ownerMember.setStatus(WorkspaceMemberStatus.ACTIVE);
        ownerMember.setJoinedAt(Instant.now());
        workspace.addMember(ownerMember);
        return FinancialWorkspaceResponse.from(workspaceRepository.save(workspace));
    }

    @Transactional
    public FinancialWorkspaceResponse update(String ownerEmail, Long id, WorkspaceUpdateRequest request) {
        FinancialWorkspace workspace = findAccessibleWorkspace(ownerEmail, id);
        ensureCanManage(ownerEmail, workspace);
        String name = normalizeNullable(request == null ? null : request.name());
        if (name != null) {
            workspace.setName(name);
        }
        return FinancialWorkspaceResponse.from(workspace);
    }

    @Transactional
    public WorkspaceInvitationResponse invite(String ownerEmail, Long workspaceId, WorkspaceInviteRequest request) {
        FinancialWorkspace workspace = findAccessibleWorkspace(ownerEmail, workspaceId);
        ensureCanManage(ownerEmail, workspace);
        if (workspace.getType() == FinancialWorkspaceType.PERSONAL) {
            throw new BusinessException("Crie um espaco familiar para convidar outras pessoas.");
        }
        User inviter = currentUserService.findUserByEmail(ownerEmail);
        String email = normalizeEmail(request.email());
        WorkspaceMemberRole role = normalizeInvitedRole(request.role());
        if (email.equalsIgnoreCase(ownerEmail)) {
            throw new BusinessException("Voce ja participa deste espaco financeiro.");
        }
        if (memberRepository.existsByWorkspaceIdAndUserEmailIgnoreCase(workspaceId, email)) {
            throw new BusinessException("Este usuario ja possui participacao neste espaco financeiro.");
        }
        WorkspaceInvitation invitation = invitationRepository
                .findByWorkspaceIdAndInviteeEmailIgnoreCaseAndStatus(workspaceId, email, WorkspaceInvitationStatus.PENDING)
                .orElseGet(WorkspaceInvitation::new);
        invitation.setWorkspace(workspace);
        invitation.setInvitedBy(inviter);
        invitation.setInviteeEmail(email);
        invitation.setRole(role);
        invitation.setStatus(WorkspaceInvitationStatus.PENDING);
        invitation.setToken(invitation.getToken() == null ? newToken() : invitation.getToken());
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return WorkspaceInvitationResponse.from(invitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> listInvitations(String ownerEmail, Long workspaceId) {
        FinancialWorkspace workspace = findAccessibleWorkspace(ownerEmail, workspaceId);
        ensureCanManage(ownerEmail, workspace);
        return invitationRepository.findByWorkspaceIdWithRelations(workspaceId)
                .stream()
                .map(WorkspaceInvitationResponse::from)
                .toList();
    }

    @Transactional
    public FinancialWorkspaceResponse acceptInvitation(String ownerEmail, String token) {
        WorkspaceInvitation invitation = invitationRepository.findByTokenWithRelations(normalizeRequired(token, "Token de convite obrigatorio."))
                .orElseThrow(() -> new ResourceNotFoundException("Convite nao encontrado."));
        if (invitation.getStatus() != WorkspaceInvitationStatus.PENDING) {
            throw new BusinessException("Este convite nao esta mais pendente.");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(WorkspaceInvitationStatus.EXPIRED);
            throw new BusinessException("Este convite expirou.");
        }
        if (!invitation.getInviteeEmail().equalsIgnoreCase(ownerEmail)) {
            throw new BusinessException("Este convite pertence a outro e-mail.");
        }
        User user = currentUserService.findUserByEmail(ownerEmail);
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserEmail(invitation.getWorkspace().getId(), ownerEmail)
                .orElseGet(WorkspaceMember::new);
        member.setWorkspace(invitation.getWorkspace());
        member.setUser(user);
        member.setRole(invitation.getRole());
        member.setStatus(WorkspaceMemberStatus.ACTIVE);
        member.setInvitedBy(invitation.getInvitedBy());
        member.setJoinedAt(Instant.now());
        member.setRemovedAt(null);
        memberRepository.save(member);
        invitation.setStatus(WorkspaceInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        return FinancialWorkspaceResponse.from(findAccessibleWorkspace(ownerEmail, invitation.getWorkspace().getId()));
    }

    @Transactional
    public FinancialWorkspaceResponse updateMemberRole(String ownerEmail, Long workspaceId, Long memberId, WorkspaceMemberRoleUpdateRequest request) {
        FinancialWorkspace workspace = findAccessibleWorkspace(ownerEmail, workspaceId);
        ensureCanManage(ownerEmail, workspace);
        WorkspaceMember member = workspace.getMembers().stream()
                .filter(item -> item.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado."));
        if (member.getRole() == WorkspaceMemberRole.OWNER) {
            throw new BusinessException("O proprietario do espaco nao pode ter o papel alterado por este fluxo.");
        }
        member.setRole(normalizeInvitedRole(request.role()));
        return FinancialWorkspaceResponse.from(workspace);
    }

    @Transactional
    public FinancialWorkspaceResponse removeMember(String ownerEmail, Long workspaceId, Long memberId) {
        FinancialWorkspace workspace = findAccessibleWorkspace(ownerEmail, workspaceId);
        ensureCanManage(ownerEmail, workspace);
        WorkspaceMember member = workspace.getMembers().stream()
                .filter(item -> item.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado."));
        if (member.getRole() == WorkspaceMemberRole.OWNER) {
            throw new BusinessException("O proprietario nao pode ser removido do espaco.");
        }
        member.setStatus(WorkspaceMemberStatus.REMOVED);
        member.setRemovedAt(Instant.now());
        return FinancialWorkspaceResponse.from(workspace);
    }

    @Transactional
    public FinancialWorkspace createPersonalWorkspaceFor(User user) {
        return workspaceRepository.findByOwnerEmailIgnoreCaseAndTypeAndActiveTrue(user.getEmail(), FinancialWorkspaceType.PERSONAL)
                .orElseGet(() -> {
                    FinancialWorkspace workspace = new FinancialWorkspace();
                    workspace.setOwner(user);
                    workspace.setName("Espaco pessoal de " + firstName(user));
                    workspace.setType(FinancialWorkspaceType.PERSONAL);
                    workspace.setActive(true);
                    WorkspaceMember member = new WorkspaceMember();
                    member.setUser(user);
                    member.setRole(WorkspaceMemberRole.OWNER);
                    member.setStatus(WorkspaceMemberStatus.ACTIVE);
                    workspace.addMember(member);
                    return workspaceRepository.save(workspace);
                });
    }

    private FinancialWorkspace findAccessibleWorkspace(String ownerEmail, Long id) {
        return workspaceRepository.findAccessibleByIdAndUserEmail(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Espaco financeiro nao encontrado."));
    }

    private void ensureCanManage(String ownerEmail, FinancialWorkspace workspace) {
        WorkspaceMember member = workspace.getMembers().stream()
                .filter(item -> item.getUser().getEmail().equalsIgnoreCase(ownerEmail))
                .filter(item -> item.getStatus() == WorkspaceMemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Espaco financeiro nao encontrado."));
        if (member.getRole() != WorkspaceMemberRole.OWNER && member.getRole() != WorkspaceMemberRole.ADMIN) {
            throw new BusinessException("Voce nao tem permissao para gerenciar este espaco financeiro.");
        }
    }

    private WorkspaceMemberRole normalizeInvitedRole(WorkspaceMemberRole role) {
        if (role == null || role == WorkspaceMemberRole.OWNER) {
            return WorkspaceMemberRole.MEMBER;
        }
        return role;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return normalizeRequired(value, "O e-mail do convidado e obrigatorio.").toLowerCase(Locale.ROOT);
    }

    private String firstName(User user) {
        String name = user.getName() == null || user.getName().isBlank() ? user.getEmail() : user.getName();
        return name.split("\\s+")[0];
    }
}
