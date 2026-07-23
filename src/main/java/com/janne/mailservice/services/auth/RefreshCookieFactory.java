package com.janne.mailservice.services.auth;

import com.janne.mailservice.configuration.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the {@code refreshToken} httpOnly cookie centrally. The cookie is scoped to {@code
 * /api/v1/auth/token} (the only endpoint that exchanges it for an identity token).
 *
 * <p>Two SameSite variants exist deliberately:
 *
 * <ul>
 *   <li>{@link #createStrict} — password login. The request originates from the SPA itself, so
 *       {@code SameSite=Strict} is the tightest safe setting.
 *   <li>{@link #createLax} — the OAuth callback. That callback is reached via a top-level redirect
 *       back from the identity provider (a cross-site navigation), and a {@code Strict} cookie is
 *       withheld on such navigations; {@code SameSite=Lax} is required for the session to survive
 *       the redirect while still blocking cross-site sub-requests.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";

    private static final String COOKIE_PATH = "/api/v1/auth/token";
    private static final String SAME_SITE_STRICT = "Strict";
    private static final String SAME_SITE_LAX = "Lax";
    private static final int MILLISECONDS_IN_SECOND = 1000;

    private final JwtProperties jwtProperties;

    /** Refresh cookie for password login / registration (SameSite=Strict). */
    public ResponseCookie createStrict(String refreshToken) {
        return base(refreshToken, SAME_SITE_STRICT).maxAge(maxAgeSeconds()).build();
    }

    /** Refresh cookie for the OAuth callback's cross-site top-level redirect (SameSite=Lax). */
    public ResponseCookie createLax(String refreshToken) {
        return base(refreshToken, SAME_SITE_LAX).maxAge(maxAgeSeconds()).build();
    }

    /** An immediately-expiring cookie that clears any existing refresh cookie (logout). */
    public ResponseCookie expire() {
        return base("", SAME_SITE_STRICT).maxAge(0).build();
    }

    private long maxAgeSeconds() {
        return jwtProperties.getRefreshTokenExpirationTime() / MILLISECONDS_IN_SECOND;
    }

    private ResponseCookie.ResponseCookieBuilder base(String value, String sameSite) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(false)
                .path(COOKIE_PATH)
                .sameSite(sameSite);
    }
}
