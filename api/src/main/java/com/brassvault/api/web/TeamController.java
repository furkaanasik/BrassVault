package com.brassvault.api.web;

import com.brassvault.api.repo.TeamRepository;
import com.brassvault.api.security.AuthPrincipal;
import com.brassvault.api.web.dto.Dtos.TeamResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository teamRepository;

    public TeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /** Only teams the requesting user is a member of — enforced by the query itself. */
    @GetMapping
    public List<TeamResponse> myTeams(@AuthenticationPrincipal AuthPrincipal principal) {
        return teamRepository.findAllByMemberUserId(principal.id()).stream()
                .map(TeamResponse::from)
                .toList();
    }
}
