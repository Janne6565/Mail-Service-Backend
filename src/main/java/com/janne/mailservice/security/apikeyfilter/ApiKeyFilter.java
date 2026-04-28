package com.janne.mailservice.security.apikeyfilter;

import com.janne.mailservice.entity.ApiKeyEntity;
import com.janne.mailservice.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String PREFIX = "mk_";
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer " + PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = header.substring(7); // strip "Bearer "
        String[] parts = rawKey.split("_", 3); // mk _ <uuid> _ <secret>
        if (parts.length != 3) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Malformed API key");
            return;
        }

        String keyUuid = parts[1];
        String secret = parts[2];

        ApiKeyEntity key = apiKeyRepository.findById(keyUuid).orElse(null);
        if (key == null
                || key.isRevoked()
                || !passwordEncoder.matches(secret, key.getSecretHash())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new ApiKeyAuthenticationToken(
                                key.getUuid(), key.getSmtpConnectionUuid(), key.getScopes()));

        filterChain.doFilter(request, response);
    }
}
