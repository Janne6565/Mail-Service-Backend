package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.ConnectionAccessEntity;
import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.SmtpConnectionEntity;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.CreateSmtpConnectionDto;
import com.janne.mailservice.model.action.GrantAccessDto;
import com.janne.mailservice.model.action.UpdateSmtpConnectionDto;
import com.janne.mailservice.model.core.ConnectionAccessDto;
import com.janne.mailservice.model.core.SmtpConnectionDto;
import com.janne.mailservice.repository.ApiKeyRepository;
import com.janne.mailservice.repository.ConnectionAccessRepository;
import com.janne.mailservice.repository.SmtpConnectionRepository;
import com.janne.mailservice.repository.UserRepository;
import com.janne.mailservice.services.crypto.SmtpPasswordCipher;
import com.janne.mailservice.services.mail.MailDispatcher;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpConnectionService {

    private final SmtpConnectionRepository smtpConnectionRepository;
    private final ConnectionAccessRepository connectionAccessRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final SmtpPasswordCipher cipher;
    private final MailDispatcher mailDispatcher;

    // ── Access helpers ────────────────────────────────────────────────────────

    private boolean isAdmin(UserEntity user) {
        return user.getRole() == Role.ADMIN;
    }

    /**
     * Returns the connection if the user may read/use it (owner, granted, or admin).
     * Throws 404 so that non-owners cannot enumerate connections they have no access to.
     */
    public SmtpConnectionEntity requireAccess(String uuid, UserEntity user) {
        SmtpConnectionEntity conn =
                smtpConnectionRepository
                        .findById(uuid)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Connection not found"));

        if (isAdmin(user)
                || conn.getOwnerUserUuid().equals(user.getUuid())
                || connectionAccessRepository.existsByConnectionUuidAndUserUuid(
                        uuid, user.getUuid())) {
            return conn;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found");
    }

    /**
     * Returns the connection if the user may edit/delete/manage access (owner or admin).
     */
    public SmtpConnectionEntity requireManage(String uuid, UserEntity user) {
        SmtpConnectionEntity conn =
                smtpConnectionRepository
                        .findById(uuid)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Connection not found"));

        if (isAdmin(user) || conn.getOwnerUserUuid().equals(user.getUuid())) {
            return conn;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only the connection owner or an admin may do this");
    }

    // ── Connection CRUD ───────────────────────────────────────────────────────

    public List<SmtpConnectionDto> getConnectionsForUser(UserEntity user) {
        if (isAdmin(user)) {
            return smtpConnectionRepository.findAll().stream().map(this::toDto).toList();
        }

        List<SmtpConnectionEntity> owned =
                smtpConnectionRepository.findAllByOwnerUserUuid(user.getUuid());

        List<String> grantedConnectionUuids =
                connectionAccessRepository.findAllByUserUuid(user.getUuid()).stream()
                        .map(ConnectionAccessEntity::getConnectionUuid)
                        .toList();
        List<SmtpConnectionEntity> granted =
                smtpConnectionRepository.findAllById(grantedConnectionUuids);

        return Stream.concat(owned.stream(), granted.stream()).map(this::toDto).toList();
    }

    public SmtpConnectionDto getConnectionDto(String uuid, UserEntity user) {
        return toDto(requireAccess(uuid, user));
    }

    public SmtpConnectionDto createConnection(String userUuid, CreateSmtpConnectionDto dto) {
        SmtpConnectionEntity entity =
                SmtpConnectionEntity.builder()
                        .ownerUserUuid(userUuid)
                        .label(dto.getLabel())
                        .host(dto.getHost())
                        .port(dto.getPort())
                        .username(dto.getUsername())
                        .passwordCiphertext(cipher.encrypt(dto.getPassword()))
                        .fromAddress(dto.getFromAddress())
                        .useStartTls(dto.isUseStartTls())
                        .build();
        return toDto(smtpConnectionRepository.save(entity));
    }

    public SmtpConnectionDto updateConnection(
            String uuid, UserEntity user, UpdateSmtpConnectionDto dto) {
        SmtpConnectionEntity entity = requireManage(uuid, user);
        if (dto.getLabel() != null) entity.setLabel(dto.getLabel());
        if (dto.getHost() != null) entity.setHost(dto.getHost());
        if (dto.getPort() != null) entity.setPort(dto.getPort());
        if (dto.getUsername() != null) entity.setUsername(dto.getUsername());
        if (dto.getPassword() != null)
            entity.setPasswordCiphertext(cipher.encrypt(dto.getPassword()));
        if (dto.getFromAddress() != null) entity.setFromAddress(dto.getFromAddress());
        if (dto.getUseStartTls() != null) entity.setUseStartTls(dto.getUseStartTls());
        return toDto(smtpConnectionRepository.save(entity));
    }

    @Transactional
    public void deleteConnection(String uuid, UserEntity user) {
        SmtpConnectionEntity entity = requireManage(uuid, user);
        apiKeyRepository.deleteAllBySmtpConnectionUuid(uuid);
        connectionAccessRepository.deleteAllByConnectionUuid(uuid);
        smtpConnectionRepository.delete(entity);
    }

    public void testConnection(String uuid, UserEntity user) {
        SmtpConnectionEntity entity = requireAccess(uuid, user);
        try {
            JavaMailSenderImpl sender = (JavaMailSenderImpl) mailDispatcher.buildSender(entity);
            sender.testConnection();
        } catch (Exception e) {
            log.warn("SMTP test failed for connection {}: {}", uuid, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "SMTP connection failed: " + e.getMessage());
        }
    }

    // ── Access management ─────────────────────────────────────────────────────

    public List<ConnectionAccessDto> listAccess(String uuid, UserEntity user) {
        requireManage(uuid, user);
        return connectionAccessRepository.findAllByConnectionUuid(uuid).stream()
                .map(
                        access -> {
                            String username =
                                    userRepository
                                            .findById(access.getUserUuid())
                                            .map(UserEntity::getUsername)
                                            .orElse("<deleted>");
                            return ConnectionAccessDto.builder()
                                    .userUuid(access.getUserUuid())
                                    .username(username)
                                    .grantedAt(access.getGrantedAt())
                                    .build();
                        })
                .toList();
    }

    public ConnectionAccessDto grantAccess(String uuid, UserEntity user, GrantAccessDto dto) {
        SmtpConnectionEntity conn = requireManage(uuid, user);

        if (conn.getOwnerUserUuid().equals(dto.getUserUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot grant access to the connection owner");
        }

        UserEntity target =
                userRepository
                        .findById(dto.getUserUuid())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));

        if (connectionAccessRepository.existsByConnectionUuidAndUserUuid(
                uuid, dto.getUserUuid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has access");
        }

        ConnectionAccessEntity access =
                ConnectionAccessEntity.builder()
                        .connectionUuid(uuid)
                        .userUuid(dto.getUserUuid())
                        .build();
        connectionAccessRepository.save(access);

        return ConnectionAccessDto.builder()
                .userUuid(target.getUuid())
                .username(target.getUsername())
                .grantedAt(access.getGrantedAt())
                .build();
    }

    @Transactional
    public void revokeAccess(String uuid, UserEntity user, String targetUserUuid) {
        requireManage(uuid, user);
        if (!connectionAccessRepository.existsByConnectionUuidAndUserUuid(uuid, targetUserUuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Access grant not found");
        }
        connectionAccessRepository.deleteByConnectionUuidAndUserUuid(uuid, targetUserUuid);
    }

    // ── Kept for ApiKeyService / MailService (access-level check) ─────────────

    /** @deprecated Use {@link #requireAccess(String, UserEntity)} where possible. */
    public SmtpConnectionEntity getOwnedConnection(String uuid, String userUuid) {
        return smtpConnectionRepository
                .findByUuidAndOwnerUserUuid(uuid, userUuid)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Connection not found"));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public SmtpConnectionDto toDto(SmtpConnectionEntity entity) {
        return SmtpConnectionDto.builder()
                .uuid(entity.getUuid())
                .ownerUserUuid(entity.getOwnerUserUuid())
                .label(entity.getLabel())
                .host(entity.getHost())
                .port(entity.getPort())
                .username(entity.getUsername())
                .fromAddress(entity.getFromAddress())
                .useStartTls(entity.isUseStartTls())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
