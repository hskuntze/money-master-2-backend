package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.OnboardingSetupRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.OnboardingStatusResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.TourStateRequest;
import br.com.kuntzedevprojects.money_master_2.services.UserOnboardingService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/onboarding")
public class UserOnboardingController {

    private final UserOnboardingService onboardingService;

    public UserOnboardingController(UserOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<OnboardingStatusResponse> status(Principal principal) {
        return ResponseEntity.ok(onboardingService.status(principal.getName()));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<OnboardingStatusResponse> complete(
            @Valid @RequestBody OnboardingSetupRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(onboardingService.complete(principal.getName(), request));
    }

    @PutMapping("/tour")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<OnboardingStatusResponse> updateTourState(
            @Valid @RequestBody TourStateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(onboardingService.updateTourState(principal.getName(), request));
    }

    @PostMapping("/tour/complete")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<OnboardingStatusResponse> completeTour(Principal principal) {
        return ResponseEntity.ok(onboardingService.updateTourState(principal.getName(), new TourStateRequest("COMPLETE", null)));
    }

    @PostMapping("/tour/skip")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<OnboardingStatusResponse> skipTour(Principal principal) {
        return ResponseEntity.ok(onboardingService.updateTourState(principal.getName(), new TourStateRequest("SKIP", null)));
    }
}
