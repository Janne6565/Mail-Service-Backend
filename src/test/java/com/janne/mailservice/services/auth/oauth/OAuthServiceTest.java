package com.janne.mailservice.services.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.janne.mailservice.configuration.oauth.OAuthProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Flow-level tests for the orchestration in {@link OAuthService}. User resolution is a separate
 * bean ({@link OAuthUserResolver}) with its own test.
 */
@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    private static final String PROVIDER = "authentik";
    private static final String ADMIN_GROUP = "mail-service-admins";
    private static final String USER_GROUP = "mail-service-users";

    @Mock private OAuthStateStore stateStore;
    @Mock private OAuthUserResolver userResolver;
    @Mock private OAuthProviderClient providerClient;

    private OAuthService service;

    private OAuthProperties properties() {
        OAuthProperties.Provider provider =
                new OAuthProperties.Provider(
                        "client123",
                        "secret456",
                        "https://sso.example.de/application/o/authorize/",
                        "https://sso.example.de/application/o/token/",
                        "https://sso.example.de/application/o/userinfo/",
                        "openid email profile",
                        "http://localhost:8080/api/v1/auth/oauth/authentik/callback");
        OAuthProperties properties = new OAuthProperties();
        properties.setProviders(Map.of(PROVIDER, provider));
        properties.setGroups(new OAuthProperties.Groups(ADMIN_GROUP, USER_GROUP));
        return properties;
    }

    @BeforeEach
    void setUp() {
        when(providerClient.providerName()).thenReturn(PROVIDER);
        service =
                new OAuthService(
                        properties(),
                        stateStore,
                        userResolver,
                        RestClient.builder().build(),
                        List.of(providerClient));
    }

    @Test
    void buildAuthorizationUrl_containsRequiredParams() {
        when(stateStore.issueState()).thenReturn("state-xyz");

        String url = service.buildAuthorizationUrl(PROVIDER);

        assertThat(url)
                .contains("https://sso.example.de/application/o/authorize/")
                .contains("client_id=client123")
                .contains("response_type=code")
                .contains("state=state-xyz");
    }

    @Test
    void buildAuthorizationUrl_unknownProvider_throwsBadRequest() {
        assertThatThrownBy(() -> service.buildAuthorizationUrl("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void handleLoginCallback_invalidState_throwsBadRequest() {
        when(stateStore.consumeState("bad-state")).thenReturn(false);

        assertThatThrownBy(() -> service.handleLoginCallback(PROVIDER, "code", "bad-state"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
