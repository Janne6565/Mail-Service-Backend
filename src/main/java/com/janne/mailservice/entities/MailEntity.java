package com.janne.mailservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NonNull
    private String recipient;
    @NonNull
    private String subject;
    @NonNull
    private String body;
    @NonNull
    private LocalDateTime sentDate;
    @Column
    private Boolean success = false;
    @Column
    private Boolean enableHtml = false;
}
