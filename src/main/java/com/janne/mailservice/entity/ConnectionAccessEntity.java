package com.janne.mailservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ConnectionAccessEntity.AccessId.class)
@Table(indexes = @Index(columnList = "userUuid"))
public class ConnectionAccessEntity {

    @Id private String connectionUuid;

    @Id private String userUuid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant grantedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessId implements Serializable {
        private String connectionUuid;
        private String userUuid;
    }
}
