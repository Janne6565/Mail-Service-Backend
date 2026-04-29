package com.janne.mailservice.model.core;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmtpConnectionDto {
    private String uuid;
    private String ownerUserUuid;
    private String label;
    private String host;
    private int port;
    private String username;
    private String fromAddress;
    private boolean useStartTls;
    private Instant createdAt;
}
