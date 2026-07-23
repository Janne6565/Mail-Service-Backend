package com.janne.mailservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links an external provider identity (e.g. an Authentik subject) to a local {@link UserEntity}.
 * Maps the {@code oauth_identity} table. The {@code (provider, provider_subject)} pair is unique —
 * one external account maps to at most one local user. No email column here: {@link UserEntity}
 * already owns the (unique, not-null) email.
 */
@Entity
@Table(
        name = "oauth_identity",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_oauth_identity",
                        columnNames = {"provider", "provider_subject"}))
@Getter
@Setter
@NoArgsConstructor
public class OAuthIdentityEntity {

    @Id
    @Column(length = 36)
    private String uuid;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public OAuthIdentityEntity(UserEntity user, String provider, String providerSubject) {
        this.uuid = UUID.randomUUID().toString();
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.createdAt = Instant.now();
    }
}
