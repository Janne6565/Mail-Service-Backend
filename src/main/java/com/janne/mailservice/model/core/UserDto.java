package com.janne.mailservice.model.core;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private String uuid;
    private String username;
    private String email;
    private String role;
    private Instant createdAt;
}
