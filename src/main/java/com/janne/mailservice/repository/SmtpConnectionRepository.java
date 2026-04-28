package com.janne.mailservice.repository;

import com.janne.mailservice.entity.SmtpConnectionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpConnectionRepository extends JpaRepository<SmtpConnectionEntity, String> {

    List<SmtpConnectionEntity> findAllByOwnerUserUuid(String ownerUserUuid);

    Optional<SmtpConnectionEntity> findByUuidAndOwnerUserUuid(String uuid, String ownerUserUuid);
}
