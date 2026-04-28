package com.janne.mailservice.repository;

import com.janne.mailservice.entity.AppSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSettingEntity, String> {}
