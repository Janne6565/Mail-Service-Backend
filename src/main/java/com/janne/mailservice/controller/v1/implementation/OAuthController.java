package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.configuration.web.FrontendProperties;
import com.janne.mailservice.controller.v1.schema.OAuthApi;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.exception.OAuthAccessDeniedException;
import com.janne.mailservice.services.auth.AuthService;
import com.janne.mailservice.services.auth.RefreshCookieFactory;
import com.janne.mailservice.services.auth.oauth.OAuthService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the {@link OAuthApi} contract. On a successful callback it issues the Mail Service's
 * own refresh token for the resolved user and sets the {@code refreshToken} cookie (SameSite=Lax
 * for the cross-site top-level redirect), then redirects to the dashboard; every failure redirects
 * to the login page with an {@code oauthError} code instead of leaking a stack trace.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController implements OAuthApi {

    private static final String ERROR_NO_ACCESS = "noAccess";
    private static final String ERROR_GENERIC = "true";

    private final OAuthService oAuthService;
    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;
    private final FrontendProperties frontendProperties;

    @Override
    public ResponseEntity<Void> authorize(String provider) {
        try {
            String url = oAuthService.buildAuthorizationUrl(provider);
            return redirect(url);
        } catch (RuntimeException ex) {
            log.warn("Failed to start OAuth authorize for provider {}", provider, ex);
            return redirect(loginError(ERROR_GENERIC));
        }
    }

    @Override
    public ResponseEntity<Void> callback(String provider, String code, String state) {
        UserEntity user;
        try {
            user = oAuthService.handleLoginCallback(provider, code, state);
        } catch (OAuthAccessDeniedException ex) {
            log.warn("OAuth login denied for provider {}: {}", provider, ex.getReason());
            return redirect(loginError(ERROR_NO_ACCESS));
        } catch (RuntimeException ex) {
            log.error("OAuth callback failed for provider {}", provider, ex);
            return redirect(loginError(ERROR_GENERIC));
        }

        String refreshToken = authService.generateRefreshToken(user.getUuid());
        ResponseCookie cookie = refreshCookieFactory.createLax(refreshToken);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .location(URI.create(frontendProperties.getUrl() + "/dashboard"))
                .build();
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private String loginError(String code) {
        return frontendProperties.getUrl() + "/login?oauthError=" + code;
    }
}
