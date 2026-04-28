package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateSettingsDto {

    @Min(1)
    @Max(3650)
    private int mailRetentionDays;
}
