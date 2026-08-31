package com.brassvault.api.web;

import com.brassvault.api.domain.User;
import com.brassvault.api.repo.TeamRepository;
import com.brassvault.api.repo.UserRepository;
import com.brassvault.api.security.AuthPrincipal;
import com.brassvault.api.service.AuthService;
import com.brassvault.api.web.dto.Dtos.ChangePasswordRequest;
import com.brassvault.api.web.dto.Dtos.MeResponse;
import com.brassvault.api.web.dto.Dtos.TeamResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AuthService authService;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public MeController(AuthService authService, TeamRepository teamRepository,
                        UserRepository userRepository) {
        this.authService = authService;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        List<TeamResponse> teams = teamRepository.findAllByMemberUserId(principal.id()).stream()
                .map(TeamResponse::from)
                .toList();
        String fullName = userRepository.findById(principal.id())
                .map(User::getFullName)
                .orElse(null);
        return new MeResponse(principal.id(), principal.email(), fullName, principal.role(),
                principal.mustChangePassword(), teams);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest body) {
        authService.changeOwnPassword(principal.id(), body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
