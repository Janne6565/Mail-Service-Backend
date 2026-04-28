package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSmtpConnectionDto {

    @NotBlank
    @Size(max = 100)
    private String label;

    @NotBlank private String host;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer port;

    @NotBlank private String username;

    @NotBlank private String password;

    @NotBlank @Email private String fromAddress;

    private boolean useStartTls = true;
}
