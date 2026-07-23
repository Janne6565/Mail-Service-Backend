package com.janne.mailservice.configuration.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Frontend location, bound from {@code mailservice.frontend}. Used by the OAuth callback to
 * redirect the browser back into the SPA (dashboard on success, login with an {@code oauthError} on
 * failure).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailservice.frontend")
public class FrontendProperties {
    private String url;
}
