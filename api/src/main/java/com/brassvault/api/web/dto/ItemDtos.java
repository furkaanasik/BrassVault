package com.brassvault.api.web.dto;

import com.brassvault.api.domain.AuditLog;
import com.brassvault.api.domain.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class ItemDtos {

    private ItemDtos() {
    }

    public record CreateItemRequest(@NotNull Long teamId,
                                    @NotBlank @Size(max = 255) String title,
                                    @Size(max = 255) String username,
                                    @NotBlank String password,
                                    @Size(max = 1024) String url,
                                    String notes) {
    }

    public record CreatePersonalItemRequest(@NotBlank @Size(max = 255) String title,
                                            @Size(max = 255) String username,
                                            @NotBlank String password,
                                            @Size(max = 1024) String url,
                                            String notes) {
    }

    public record UpdateItemRequest(@NotBlank @Size(max = 255) String title,
                                    @Size(max = 255) String username,
                                    String password,
                                    @Size(max = 1024) String url,
                                    String notes) {
    }

    /** List/detail view — never carries the password. */
    public record ItemResponse(Long id, Long teamId, String title, String username,
                               String url, String notes,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        public static ItemResponse from(Item i) {
            return new ItemResponse(i.getId(), i.getTeamId(), i.getTitle(), i.getUsername(),
                    i.getUrl(), i.getNotes(), i.getCreatedAt(), i.getUpdatedAt());
        }
    }

    public record SecretResponse(String password) {
    }

    public record AuditLogResponse(Long id, String userEmail, String itemTitle, String teamName,
                                   String action, String ipAddress, OffsetDateTime createdAt) {
        public static AuditLogResponse from(AuditLog a) {
            return new AuditLogResponse(a.getId(), a.getUserEmail(), a.getItemTitle(), a.getTeamName(),
                    a.getAction().name(), a.getIpAddress(), a.getCreatedAt());
        }
    }
}
