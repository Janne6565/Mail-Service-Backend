package com.janne.mailservice.model.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApiKeyDto {

    @NotBlank
    @Size(max = 100)
    private String label;

    private boolean enableReadMails = false;
}
