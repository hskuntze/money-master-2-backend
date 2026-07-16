package br.com.kuntzedevprojects.money_master_2.controllers.workspace;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.workspace.FinancialWorkspaceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceInvitationResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceInviteRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceMemberRoleUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.workspace.WorkspaceUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.workspace.FinancialWorkspaceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/workspaces")
public class FinancialWorkspaceController {

    private final FinancialWorkspaceService workspaceService;

    public FinancialWorkspaceController(FinancialWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FinancialWorkspaceResponse>> list(Principal principal) {
        return ResponseEntity.ok(workspaceService.list(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinancialWorkspaceResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(workspaceService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialWorkspaceResponse> create(@Valid @RequestBody WorkspaceCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialWorkspaceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(workspaceService.update(principal.getName(), id, request));
    }

    @PostMapping("/{id}/invitations")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<WorkspaceInvitationResponse> invite(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceInviteRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.invite(principal.getName(), id, request));
    }

    @GetMapping("/{id}/invitations")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<List<WorkspaceInvitationResponse>> invitations(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(workspaceService.listInvitations(principal.getName(), id));
    }

    @PostMapping("/invitations/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinancialWorkspaceResponse> accept(@PathVariable String token, Principal principal) {
        return ResponseEntity.ok(workspaceService.acceptInvitation(principal.getName(), token));
    }

    @PutMapping("/{workspaceId}/members/{memberId}/role")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialWorkspaceResponse> updateMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody WorkspaceMemberRoleUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(workspaceService.updateMemberRole(principal.getName(), workspaceId, memberId, request));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialWorkspaceResponse> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            Principal principal
    ) {
        return ResponseEntity.ok(workspaceService.removeMember(principal.getName(), workspaceId, memberId));
    }
}
