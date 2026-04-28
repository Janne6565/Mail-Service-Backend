package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.AdminSettingsApi;
import com.janne.mailservice.model.action.UpdateSettingsDto;
import com.janne.mailservice.model.core.AppSettingsDto;
import com.janne.mailservice.services.core.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminSettingsController implements AdminSettingsApi {

    private final SettingsService settingsService;

    @Override
    public ResponseEntity<AppSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @Override
    public ResponseEntity<AppSettingsDto> updateSettings(UpdateSettingsDto dto) {
        return ResponseEntity.ok(settingsService.adminUpdateSettings(dto));
    }
}
