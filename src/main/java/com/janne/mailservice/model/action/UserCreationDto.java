package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreationDto {

    @NotBlank
    @Size(min = 3, max = 32)
    private String username;

    @NotBlank @Email private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;
}
