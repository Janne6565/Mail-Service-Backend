package com.janne.mailservice.services.auth.oauth;

import java.util.List;

/**
 * The provider's view of the logging-in user, normalised across providers. {@code groups} may be
 * empty when the provider omits them; the strict group gate then rejects the login. {@code
 * emailVerified} reflects the OIDC {@code email_verified} claim; auto-linking to a pre-existing
 * local account by email requires it to be {@code true} (fail closed).
 */
public record OAuthUserInfo(
        String subject,
        String email,
        boolean emailVerified,
        String username,
        List<String> groups) {}
