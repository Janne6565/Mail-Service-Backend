package com.janne.mailservice.repository;

import com.janne.mailservice.entity.ApiKeyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, String> {

    List<ApiKeyEntity> findAllBySmtpConnectionUuid(String smtpConnectionUuid);

    void deleteAllBySmtpConnectionUuid(String smtpConnectionUuid);
}
