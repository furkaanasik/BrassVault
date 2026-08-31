package com.brassvault.api.web.admin;

import com.brassvault.api.security.AuthPrincipal;
import com.brassvault.api.service.TeamAdminService;
import com.brassvault.api.web.dto.Dtos.AddMemberRequest;
import com.brassvault.api.web.dto.Dtos.CreateTeamRequest;
import com.brassvault.api.web.dto.Dtos.TeamResponse;
import com.brassvault.api.web.dto.Dtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private final TeamAdminService teamAdminService;

    public AdminTeamController(TeamAdminService teamAdminService) {
        this.teamAdminService = teamAdminService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamResponse.from(teamAdminService.createTeam(body.name(), body.description())));
    }

    @GetMapping
    public List<TeamResponse> list() {
        return teamAdminService.listTeams().stream().map(TeamResponse::from).toList();
    }

    @GetMapping("/{id}/members")
    public List<UserResponse> members(@PathVariable Long id) {
        return teamAdminService.listMembers(id).stream().map(UserResponse::from).toList();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long id,
                                          @Valid @RequestBody AddMemberRequest body,
                                          @AuthenticationPrincipal AuthPrincipal principal,
                                          HttpServletRequest request) {
        teamAdminService.addMember(id, body.userId(), principal.email(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId,
                                             @AuthenticationPrincipal AuthPrincipal principal,
                                             HttpServletRequest request) {
        teamAdminService.removeMember(id, userId, principal.email(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
