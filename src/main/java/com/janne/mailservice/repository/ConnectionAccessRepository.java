package com.janne.mailservice.repository;

import com.janne.mailservice.entity.ConnectionAccessEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionAccessRepository
        extends JpaRepository<ConnectionAccessEntity, ConnectionAccessEntity.AccessId> {

    List<ConnectionAccessEntity> findAllByConnectionUuid(String connectionUuid);

    List<ConnectionAccessEntity> findAllByUserUuid(String userUuid);

    boolean existsByConnectionUuidAndUserUuid(String connectionUuid, String userUuid);

    void deleteByConnectionUuidAndUserUuid(String connectionUuid, String userUuid);

    void deleteAllByConnectionUuid(String connectionUuid);

    void deleteAllByUserUuid(String userUuid);
}
