package com.brassvault.api.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "team_members")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TeamMember {

    @EmbeddedId
    private TeamMemberId id;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    public TeamMember(Long teamId, Long userId) {
        this.id = new TeamMemberId(teamId, userId);
    }
}
