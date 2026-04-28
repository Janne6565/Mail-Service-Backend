package com.janne.mailservice.model.core;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InviteDto {
    private String uuid;
    private String token;
    private String createdByUserUuid;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
    private boolean used;
    private boolean expired;
}
