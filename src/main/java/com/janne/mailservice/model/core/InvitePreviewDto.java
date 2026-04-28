package com.janne.mailservice.model.core;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvitePreviewDto {
    private boolean valid;
    private Instant expiresAt;
    private String reason;
}
