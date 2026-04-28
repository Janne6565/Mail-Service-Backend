package com.janne.mailservice.model.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}
