package com.janne.mailservice.model.core;

import com.janne.mailservice.entity.ApiKeyScope;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiKeyCreationResultDto {
    private String uuid;
    private String label;
    private Set<ApiKeyScope> scopes;
    private Instant createdAt;

    /** Full API key string shown exactly once — format: {@code mk_<uuid>_<secret>} */
    private String apiKey;
}
