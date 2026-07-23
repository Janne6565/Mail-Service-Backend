package com.janne.mailservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.repository.UserRepository;
import com.janne.mailservice.security.jwtfilter.JwtTokenBody;
import com.janne.mailservice.security.jwtfilter.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression test for the admin route matcher. {@code WebMvcConfig} prefixes all controllers with
 * {@code /api}, so the security rule must guard {@code /api/v1/admin/**}; a matcher missing the
 * prefix silently never fires and lets any authenticated USER reach admin endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtils jwtUtils;

    private String identityTokenFor(String username, Role role) {
        UserEntity user =
                userRepository.save(
                        UserEntity.builder()
                                .username(username)
                                .email(username + "@example.com")
                                .passwordHash(null)
                                .role(role)
                                .build());
        return jwtUtils.generateToken(JwtTokenBody.forIdentityToken(user));
    }

    @Test
    void adminEndpoint_withUserRoleToken_isForbidden() throws Exception {
        String token = identityTokenFor("part0user", Role.USER);

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withAdminRoleToken_isAllowed() throws Exception {
        String token = identityTokenFor("part0admin", Role.ADMIN);

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }
}
