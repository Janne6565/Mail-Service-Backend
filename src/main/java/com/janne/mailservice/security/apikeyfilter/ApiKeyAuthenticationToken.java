package com.janne.mailservice.security.apikeyfilter;

import com.janne.mailservice.entity.ApiKeyScope;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKeyUuid;
    private final String smtpConnectionUuid;

    public ApiKeyAuthenticationToken(
            String apiKeyUuid, String smtpConnectionUuid, Set<ApiKeyScope> scopes) {
        super(toAuthorities(scopes));
        this.apiKeyUuid = apiKeyUuid;
        this.smtpConnectionUuid = smtpConnectionUuid;
        setAuthenticated(true);
    }

    private static Collection<SimpleGrantedAuthority> toAuthorities(Set<ApiKeyScope> scopes) {
        return scopes.stream()
                .map(s -> new SimpleGrantedAuthority("SCOPE_" + s.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getPrincipal() {
        return apiKeyUuid;
    }

    public String getSmtpConnectionUuid() {
        return smtpConnectionUuid;
    }
}
