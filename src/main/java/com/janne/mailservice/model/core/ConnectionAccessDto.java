package com.janne.mailservice.model.core;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionAccessDto {
    private String userUuid;
    private String username;
    private Instant grantedAt;
}
