package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSmtpConnectionDto {

    @Size(max = 100)
    private String label;

    private String host;

    @Min(1)
    @Max(65535)
    private Integer port;

    private String username;

    private String password;

    @Email private String fromAddress;

    private Boolean useStartTls;
}
