package com.janne.mailservice.repository;

import com.janne.mailservice.entity.MailEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MailRepository extends JpaRepository<MailEntity, String> {

    Page<MailEntity> findAllBySmtpConnectionUuid(String smtpConnectionUuid, Pageable pageable);

    Page<MailEntity> findAllBySmtpConnectionUuidAndApiKeyUuid(
            String smtpConnectionUuid, String apiKeyUuid, Pageable pageable);

    Optional<MailEntity> findByUuidAndSmtpConnectionUuid(String uuid, String smtpConnectionUuid);

    @Modifying
    @Query("DELETE FROM MailEntity m WHERE m.sentAt < :cutoff")
    int deleteAllBySentAtBefore(Instant cutoff);
}
