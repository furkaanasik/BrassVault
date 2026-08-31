package com.brassvault.api.service;

import com.brassvault.api.domain.*;
import com.brassvault.api.repo.TeamMemberRepository;
import com.brassvault.api.repo.TeamRepository;
import com.brassvault.api.repo.UserRepository;
import com.brassvault.api.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamAdminService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditService auditService;

    public TeamAdminService(TeamRepository teamRepository, UserRepository userRepository,
                            TeamMemberRepository teamMemberRepository, AuditService auditService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Team createTeam(String name, String description) {
        if (teamRepository.existsByName(name)) {
            throw new IllegalArgumentException("A team with this name already exists");
        }
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        return teamRepository.save(team);
    }

    public List<Team> listTeams() {
        return teamRepository.findAll();
    }

    public List<User> listMembers(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));
        return userRepository.findAllByTeamId(teamId);
    }

    @Transactional
    public void addMember(Long teamId, Long userId, String adminEmail, String ipAddress) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        TeamMemberId id = new TeamMemberId(teamId, userId);
        if (!teamMemberRepository.existsById(id)) {
            teamMemberRepository.save(new TeamMember(teamId, userId));
            auditService.record(adminEmail, AuditAction.ADD_MEMBER, null, team.getName(), ipAddress);
        }
    }

    @Transactional
    public void removeMember(Long teamId, Long userId, String adminEmail, String ipAddress) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));
        TeamMemberId id = new TeamMemberId(teamId, userId);
        if (teamMemberRepository.existsById(id)) {
            teamMemberRepository.deleteById(id);
            auditService.record(adminEmail, AuditAction.REMOVE_MEMBER, null, team.getName(), ipAddress);
        }
    }
}
