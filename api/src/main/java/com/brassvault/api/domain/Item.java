package com.brassvault.api.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id")
    private Long teamId;

    // reserved for v2 personal vault; never written by v1 endpoints
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(nullable = false)
    private String title;

    private String username;

    @ToString.Exclude
    @Column(name = "encrypted_password", nullable = false)
    private byte[] encryptedPassword;

    @Column(length = 1024)
    private String url;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
