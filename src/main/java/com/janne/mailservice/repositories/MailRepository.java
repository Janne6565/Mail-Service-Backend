package com.janne.mailservice.repositories;

import com.janne.mailservice.entities.MailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailRepository extends JpaRepository<MailEntity, String> {
}
