package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.profile.UserFinancialProfileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.profile.UserFinancialProfileResponse;
import br.com.kuntzedevprojects.money_master_2.services.UserFinancialProfileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/financial-profile")
public class UserFinancialProfileController {

	private final UserFinancialProfileService profileService;

	public UserFinancialProfileController(UserFinancialProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('FINANCE_READ')")
	public ResponseEntity<UserFinancialProfileResponse> get(Principal principal) {
		return ResponseEntity.ok(profileService.getOrCreate(principal.getName()));
	}

	@PutMapping
	@PreAuthorize("hasAuthority('FINANCE_MANAGE')")
	public ResponseEntity<UserFinancialProfileResponse> update(@Valid @RequestBody UserFinancialProfileRequest request,
			Principal principal) {
		return ResponseEntity.ok(profileService.update(principal.getName(), request));
	}
}
