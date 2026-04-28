package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.UpdateSettingsDto;
import com.janne.mailservice.model.core.AppSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin — Settings")
@RequestMapping("/v1/admin/settings")
public interface AdminSettingsApi {

    @Operation(summary = "Get application settings")
    @GetMapping
    ResponseEntity<AppSettingsDto> getSettings();

    @Operation(summary = "Update application settings")
    @PatchMapping
    ResponseEntity<AppSettingsDto> updateSettings(@Valid @RequestBody UpdateSettingsDto dto);
}
