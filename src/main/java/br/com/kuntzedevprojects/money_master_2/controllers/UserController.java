package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;
import java.time.Duration;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.kuntzedevprojects.money_master_2.dtos.user.RoleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserSelfUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.list());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentProfile(
            Principal principal,
            @Valid @RequestBody UserSelfUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentProfile(principal.getName(), request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadCurrentAvatar(
            Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(userService.uploadCurrentAvatar(principal.getName(), file));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserResponse> deleteCurrentAvatar(Principal principal) {
        return ResponseEntity.ok(userService.deleteCurrentAvatar(principal.getName()));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) {
        UserService.AvatarResource avatar = userService.loadAvatar(id);
        MediaType mediaType = avatar.contentType() == null || avatar.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(avatar.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(avatar.resource());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(userService.listRoles());
    }
}
