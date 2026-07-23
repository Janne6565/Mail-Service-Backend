package com.janne.mailservice.services.auth.oauth;

import com.janne.mailservice.configuration.oauth.OAuthProperties;
import com.janne.mailservice.entity.OAuthIdentityEntity;
import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.exception.OAuthAccessDeniedException;
import com.janne.mailservice.repository.OAuthIdentityRepository;
import com.janne.mailservice.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a provider identity to a local {@link UserEntity} in a single transaction: enforces the
 * strict group gate, reuses a linked identity, auto-links an existing user by email, or creates a
 * new user, and syncs the coarse role from the provider's groups on every login.
 *
 * <p>Deliberately a separate bean from {@link OAuthService}: the resolution must run inside a
 * transaction (dirty-checking role sync, atomic user+identity insert), but {@code
 * OAuthService.handleLoginCallback} wraps two remote HTTP calls and must not hold a DB connection
 * across them. Calling this across the bean boundary is what lets the {@code @Transactional} proxy
 * apply (self-invocation would silently bypass it — SPRING_BOOT.md).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUserResolver {

    private static final String USERNAME_SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int USERNAME_SUFFIX_LENGTH = 6;
    private static final String FALLBACK_USERNAME = "user";

    private final SecureRandom random = new SecureRandom();

    private final OAuthProperties properties;
    private final OAuthIdentityRepository identityRepository;
    private final UserRepository userRepository;

    /**
     * Enforces the strict group gate first, then reuses a linked identity, auto-links by email, or
     * creates a new user, and syncs the role. Runs in a single transaction so role sync flushes via
     * dirty checking and the user+identity insert is atomic.
     */
    @Transactional
    public UserEntity resolveLoginUser(String provider, OAuthUserInfo userInfo) {
        // Strict group gate BEFORE any create/login: throws when no mail-service-* group is
        // present.
        Role mappedRole = mapRole(userInfo.groups());

        Optional<OAuthIdentityEntity> existing =
                identityRepository.findByProviderAndProviderSubject(provider, userInfo.subject());
        if (existing.isPresent()) {
            UserEntity user = existing.get().getUser();
            syncRole(user, mappedRole);
            return user;
        }

        // Auto-link by email (differs from Strata, which always creates a fresh user). Safe because
        // Authentik has no open enrollment: only accounts an admin provisioned in Authentik and
        // placed in a mail-service-* group can reach this point, so matching on a verified email
        // cannot let a stranger hijack an existing account.
        String email = userInfo.email();
        if (email != null && !email.isBlank()) {
            Optional<UserEntity> byEmail = userRepository.findByEmailIgnoreCase(email);
            if (byEmail.isPresent()) {
                // Fail closed: never attach this identity to a PRE-EXISTING local account unless
                // the provider asserts the email is verified. Without this, a forged/unverified
                // email claim could hijack an existing account. Creating a brand-new user or
                // reusing an already-linked identity does not need this gate.
                if (!userInfo.emailVerified()) {
                    throw new OAuthAccessDeniedException(
                            "Cannot link to an existing account without a verified email");
                }
                UserEntity user = byEmail.get();
                identityRepository.save(
                        new OAuthIdentityEntity(user, provider, userInfo.subject()));
                syncRole(user, mappedRole);
                log.info(
                        "Linked OAuth identity to existing user '{}' via {} by email",
                        user.getUsername(),
                        provider);
                return user;
            }
        }

        // Create a brand-new user. Email is required (NOT NULL + unique on user_entity).
        if (email == null || email.isBlank()) {
            throw new OAuthAccessDeniedException(
                    "OAuth provider did not supply an email address; cannot create an account");
        }
        UserEntity user =
                UserEntity.builder()
                        .username(uniqueUsername(userInfo))
                        .email(email)
                        .passwordHash(null)
                        .role(mappedRole)
                        .build();
        userRepository.save(user);
        identityRepository.save(new OAuthIdentityEntity(user, provider, userInfo.subject()));
        log.info(
                "Created OAuth user '{}' via {} with role {}",
                user.getUsername(),
                provider,
                mappedRole);
        return user;
    }

    /** ADMIN when the admin group is present, else USER; rejects users in neither group. */
    private Role mapRole(List<String> groups) {
        boolean admin = groups.contains(properties.getGroups().getAdmin());
        boolean user = groups.contains(properties.getGroups().getUser());
        if (!admin && !user) {
            throw new OAuthAccessDeniedException("No mail-service group assigned");
        }
        return admin ? Role.ADMIN : Role.USER;
    }

    private void syncRole(UserEntity user, Role mappedRole) {
        if (user.getRole() == mappedRole) {
            return;
        }
        log.info(
                "Syncing role of '{}' from {} to {}",
                user.getUsername(),
                user.getRole(),
                mappedRole);
        user.setRole(mappedRole);
    }

    private String uniqueUsername(OAuthUserInfo userInfo) {
        String base = sanitize(userInfo.username());
        if (base.isEmpty()) {
            base = sanitize(localPart(userInfo.email()));
        }
        if (base.isEmpty()) {
            base = FALLBACK_USERNAME;
        }
        String candidate = base;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + "-" + randomSuffix();
        }
        return candidate;
    }

    private String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String localPart(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(USERNAME_SUFFIX_LENGTH);
        for (int i = 0; i < USERNAME_SUFFIX_LENGTH; i++) {
            suffix.append(
                    USERNAME_SUFFIX_ALPHABET.charAt(
                            random.nextInt(USERNAME_SUFFIX_ALPHABET.length())));
        }
        return suffix.toString();
    }
}
