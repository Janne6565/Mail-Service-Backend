package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.AppSettingEntity;
import com.janne.mailservice.entity.AppSettingKey;
import com.janne.mailservice.model.action.UpdateSettingsDto;
import com.janne.mailservice.model.core.AppSettingsDto;
import com.janne.mailservice.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AppSettingRepository appSettingRepository;

    public AppSettingsDto getSettings() {
        int retentionDays = getIntSetting(AppSettingKey.MAIL_RETENTION_DAYS, 30);
        return AppSettingsDto.builder().mailRetentionDays(retentionDays).build();
    }

    public AppSettingsDto adminUpdateSettings(UpdateSettingsDto dto) {
        setSetting(AppSettingKey.MAIL_RETENTION_DAYS, String.valueOf(dto.getMailRetentionDays()));
        return getSettings();
    }

    private int getIntSetting(AppSettingKey key, int defaultValue) {
        return appSettingRepository
                .findById(key.name())
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(defaultValue);
    }

    private void setSetting(AppSettingKey key, String value) {
        AppSettingEntity entity =
                appSettingRepository
                        .findById(key.name())
                        .orElse(AppSettingEntity.builder().settingKey(key.name()).build());
        entity.setSettingValue(value);
        appSettingRepository.save(entity);
    }
}
