package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDto {

    @Size(min = 3, max = 32)
    private String username;

    @Email private String email;
}
