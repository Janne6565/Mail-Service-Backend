package com.janne.mailservice.model.core;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailDto {
    private String uuid;
    private String smtpConnectionUuid;
    private String apiKeyUuid;
    private String recipient;
    private String subject;
    private String body;
    private boolean html;
    private Instant sentAt;
    private boolean success;
    private String errorMessage;
}
