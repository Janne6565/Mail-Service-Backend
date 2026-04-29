package com.janne.mailservice.model.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryDto {
    private String uuid;
    private String username;
}
