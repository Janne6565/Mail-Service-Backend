package com.janne.mailservice.services.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.janne.mailservice.configuration.oauth.OAuthProperties;
import com.janne.mailservice.entity.OAuthIdentityEntity;
import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.exception.OAuthAccessDeniedException;
import com.janne.mailservice.repository.OAuthIdentityRepository;
import com.janne.mailservice.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the transactional user resolution / link / creation / role-sync logic. */
@ExtendWith(MockitoExtension.class)
class OAuthUserResolverTest {

    private static final String PROVIDER = "authentik";
    private static final String ADMIN_GROUP = "mail-service-admins";
    private static final String USER_GROUP = "mail-service-users";

    @Mock private OAuthIdentityRepository identityRepository;
    @Mock private UserRepository userRepository;

    private OAuthUserResolver resolver;

    @BeforeEach
    void setUp() {
        OAuthProperties properties = new OAuthProperties();
        properties.setGroups(new OAuthProperties.Groups(ADMIN_GROUP, USER_GROUP));
        resolver = new OAuthUserResolver(properties, identityRepository, userRepository);
    }

    private OAuthUserInfo userInfo(
            String subject, String username, String email, String... groups) {
        return new OAuthUserInfo(subject, email, username, List.of(groups));
    }

    private UserEntity user(String username, String passwordHash, Role role) {
        return UserEntity.builder()
                .uuid("uuid-" + username)
                .username(username)
                .email(username + "@example.com")
                .passwordHash(passwordHash)
                .role(role)
                .build();
    }

    // --- new user creation -------------------------------------------------

    @Test
    void resolveLoginUser_newUser_createsUserWithNullPasswordAndUserRole() {
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-1", "alice", "alice@example.com", USER_GROUP));

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getPasswordHash()).isNull();
        verify(identityRepository).save(any(OAuthIdentityEntity.class));
    }

    @Test
    void resolveLoginUser_adminGroup_createsAdmin() {
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-2"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("boss@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("boss")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-2", "boss", "boss@example.com", ADMIN_GROUP));

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void resolveLoginUser_usernameCollision_appendsRandomSuffix() {
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-3"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice2@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase(
                        argThat(s -> s != null && s.startsWith("alice-"))))
                .thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-3", "alice", "alice2@example.com", USER_GROUP));

        assertThat(result.getUsername()).matches("alice-[a-z0-9]{6}");
    }

    @Test
    void resolveLoginUser_blankUsername_fallsBackToEmailLocalPart() {
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-4"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("jane")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-4", "", "jane@example.com", USER_GROUP));

        assertThat(result.getUsername()).isEqualTo("jane");
    }

    @Test
    void resolveLoginUser_noEmail_throwsAccessDeniedAndCreatesNothing() {
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-10"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                resolver.resolveLoginUser(
                                        PROVIDER, userInfo("sub-10", "noemail", null, USER_GROUP)))
                .isInstanceOf(OAuthAccessDeniedException.class);

        verify(userRepository, never()).save(any());
        verify(identityRepository, never()).save(any());
    }

    // --- auto-link by email (differs from Strata) --------------------------

    @Test
    void resolveLoginUser_emailMatchesExistingUser_linksInsteadOfCreating() {
        UserEntity existing = user("bob", "hash", Role.USER);
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-link"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("bob@example.com"))
                .thenReturn(Optional.of(existing));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-link", "bob", "bob@example.com", USER_GROUP));

        assertThat(result).isSameAs(existing);
        verify(identityRepository).save(any(OAuthIdentityEntity.class));
        // Existing user is reused, not re-created.
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveLoginUser_emailMatchesExistingUser_syncsRoleOnLink() {
        UserEntity existing = user("bob", "hash", Role.USER);
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-link2"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("bob@example.com"))
                .thenReturn(Optional.of(existing));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-link2", "bob", "bob@example.com", ADMIN_GROUP));

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        verify(identityRepository).save(any(OAuthIdentityEntity.class));
    }

    // --- strict group gate -------------------------------------------------

    @Test
    void resolveLoginUser_noMailServiceGroup_throwsAccessDeniedAndCreatesNothing() {
        assertThatThrownBy(
                        () ->
                                resolver.resolveLoginUser(
                                        PROVIDER,
                                        userInfo(
                                                "sub-5", "nobody", "x@example.com", "other-group")))
                .isInstanceOf(OAuthAccessDeniedException.class);

        verify(userRepository, never()).save(any());
        verify(identityRepository, never()).save(any());
    }

    // --- existing identity + role sync -------------------------------------

    @Test
    void resolveLoginUser_existingIdentity_syncsRoleUserToAdmin() {
        UserEntity linked = user("carol", null, Role.USER);
        OAuthIdentityEntity identity = new OAuthIdentityEntity(linked, PROVIDER, "sub-6");
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-6"))
                .thenReturn(Optional.of(identity));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-6", "carol", "carol@example.com", ADMIN_GROUP));

        assertThat(result).isSameAs(linked);
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(any());
        verify(identityRepository, never()).save(any());
    }

    @Test
    void resolveLoginUser_existingIdentity_syncsRoleAdminToUser() {
        UserEntity linked = user("dave", null, Role.ADMIN);
        OAuthIdentityEntity identity = new OAuthIdentityEntity(linked, PROVIDER, "sub-7");
        when(identityRepository.findByProviderAndProviderSubject(PROVIDER, "sub-7"))
                .thenReturn(Optional.of(identity));

        UserEntity result =
                resolver.resolveLoginUser(
                        PROVIDER, userInfo("sub-7", "dave", "dave@example.com", USER_GROUP));

        assertThat(result.getRole()).isEqualTo(Role.USER);
    }
}
