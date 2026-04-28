package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMailDto {

    @NotBlank @Email private String recipient;

    @NotBlank
    @Size(max = 500)
    private String subject;

    @NotBlank private String body;

    private boolean html;
}
