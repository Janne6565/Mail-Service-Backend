package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateInviteDto {

    @Min(1)
    @Max(365)
    private int expiresInDays = 7;

    @Size(max = 100)
    private String label;
}
