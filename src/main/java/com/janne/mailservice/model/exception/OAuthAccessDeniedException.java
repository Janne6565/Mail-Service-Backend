package com.janne.mailservice.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 403 — an OAuth login was authenticated by the provider but the user is not entitled to the Mail
 * Service (none of the required {@code mail-service-*} groups, a disabled account, or missing
 * required user info such as email). Surfaced to the frontend as {@code oauthError=noAccess}.
 */
public class OAuthAccessDeniedException extends ResponseStatusException {
    public OAuthAccessDeniedException(String reason) {
        super(HttpStatus.FORBIDDEN, reason);
    }
}
