package com.janne.mailservice.services.auth;

import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.security.apikeyfilter.ApiKeyAuthenticationToken;
import com.janne.mailservice.security.jwtfilter.AuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecurityContextService {

    public UserEntity getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AuthenticationToken token) {
            return token.getUser();
        }
        return null;
    }

    public UserEntity requireUser() {
        UserEntity user = getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return user;
    }

    public ApiKeyAuthenticationToken requireApiKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken token) {
            return token;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key required");
    }
}
