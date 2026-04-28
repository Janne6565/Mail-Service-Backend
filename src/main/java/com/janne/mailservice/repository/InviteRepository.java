package com.janne.mailservice.repository;

import com.janne.mailservice.entity.InviteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRepository extends JpaRepository<InviteEntity, String> {

    Optional<InviteEntity> findByToken(String token);
}
