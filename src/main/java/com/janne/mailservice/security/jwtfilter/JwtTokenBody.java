package com.janne.mailservice.security.jwtfilter;

import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.UserEntity;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtTokenBody {

    private String userId;
    private String username;
    private String email;
    private Role role;
    private TokenType tokenType;

    public enum TokenType {
        REFRESH_TOKEN,
        IDENTITY_TOKEN,
    }

    public static JwtTokenBody forRefreshToken(UserEntity user) {
        return JwtTokenBody.builder()
                .tokenType(TokenType.REFRESH_TOKEN)
                .userId(user.getUuid())
                .username(user.getUsername())
                .build();
    }

    public static JwtTokenBody forIdentityToken(UserEntity user) {
        return JwtTokenBody.builder()
                .tokenType(TokenType.IDENTITY_TOKEN)
                .userId(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public Map<String, Object> toClaimsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tokenType", tokenType);
        if (userId != null) map.put("userId", userId);
        if (username != null) map.put("username", username);
        if (email != null) map.put("email", email);
        if (role != null) map.put("role", role.name());
        return map;
    }
}
