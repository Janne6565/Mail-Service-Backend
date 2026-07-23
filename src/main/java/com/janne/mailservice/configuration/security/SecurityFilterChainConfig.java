package com.janne.mailservice.configuration.security;

import com.janne.mailservice.security.apikeyfilter.ApiKeyFilter;
import com.janne.mailservice.security.jwtfilter.JwtFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityFilterChainConfig {

    private final JwtFilter jwtFilter;
    private final ApiKeyFilter apiKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/v1/auth/**",
                                                "/api/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/actuator/**")
                                        .permitAll()
                                        // WebMvcConfig prefixes every @RestController with
                                        // "/api", so admin endpoints resolve to /api/v1/admin/**.
                                        // The matcher must include that prefix or the rule never
                                        // fires and any authenticated USER reaches admin routes.
                                        .requestMatchers("/api/v1/admin/**")
                                        .hasRole("ADMIN")
                                        .dispatcherTypeMatchers(DispatcherType.ASYNC)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        eh ->
                                eh.authenticationEntryPoint(
                                        (request, response, authException) ->
                                                response.sendError(
                                                        HttpServletResponse.SC_UNAUTHORIZED,
                                                        "Unauthorized")))
                .build();
    }
}
