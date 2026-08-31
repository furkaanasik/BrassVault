package com.brassvault.api.web.dto;

import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.Team;
import com.brassvault.api.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 12, max = 128) String newPassword) {
    }

    public record CreateUserRequest(@NotBlank @Email String email,
                                    @NotBlank String fullName,
                                    @NotNull Role role) {
    }

    public record UpdateUserRequest(Role role, Boolean active) {
    }

    public record CreateTeamRequest(@NotBlank String name, String description) {
    }

    public record AddMemberRequest(@NotNull Long userId) {
    }

    public record UserResponse(Long id, String email, String fullName, Role role,
                               boolean active, boolean mustChangePassword) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                    u.isActive(), u.isMustChangePassword());
        }
    }

    public record CreatedUserResponse(Long id, String email, String fullName, Role role,
                                      String tempPassword) {
    }

    public record TeamResponse(Long id, String name, String description) {
        public static TeamResponse from(Team t) {
            return new TeamResponse(t.getId(), t.getName(), t.getDescription());
        }
    }

    public record MeResponse(Long id, String email, String fullName, Role role,
                             boolean mustChangePassword, List<TeamResponse> teams) {
    }
}
