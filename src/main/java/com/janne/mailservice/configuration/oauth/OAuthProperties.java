package com.janne.mailservice.configuration.oauth;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Federated OIDC login configuration, bound from {@code mailservice.oauth}. Each entry in {@code
 * providers} is an authorization-code provider (the pilot ships a single {@code authentik} entry);
 * {@code groups} names the Authentik groups that gate access and map to the coarse role.
 *
 * <p>{@code clientId}/{@code clientSecret} are intentionally optional so the app still boots in
 * local dev without credentials — the OAuth endpoints simply won't authenticate until they are
 * supplied via environment.
 */
@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "mailservice.oauth")
public class OAuthProperties {

    private Map<String, Provider> providers = new HashMap<>();
    private Groups groups;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String scope;
        private String callbackUri;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Groups {
        private String admin;
        private String user;
    }
}
