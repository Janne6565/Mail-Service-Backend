package com.janne.mailservice.model.action;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GrantAccessDto {

    @NotBlank
    private String userUuid;
}
