package com.janne.mailservice.model.core;

import com.janne.mailservice.entity.ApiKeyScope;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiKeyDto {
    private String uuid;
    private String label;
    private Set<ApiKeyScope> scopes;
    private Instant createdAt;
    private Instant lastUsedAt;
    private boolean revoked;
}
