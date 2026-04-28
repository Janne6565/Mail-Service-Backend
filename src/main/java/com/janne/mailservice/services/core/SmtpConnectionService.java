package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.SmtpConnectionEntity;
import com.janne.mailservice.model.action.CreateSmtpConnectionDto;
import com.janne.mailservice.model.action.UpdateSmtpConnectionDto;
import com.janne.mailservice.model.core.SmtpConnectionDto;
import com.janne.mailservice.repository.ApiKeyRepository;
import com.janne.mailservice.repository.SmtpConnectionRepository;
import com.janne.mailservice.services.crypto.SmtpPasswordCipher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SmtpConnectionService {

    private final SmtpConnectionRepository smtpConnectionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final SmtpPasswordCipher cipher;

    public List<SmtpConnectionDto> getConnectionsForUser(String userUuid) {
        return smtpConnectionRepository.findAllByOwnerUserUuid(userUuid).stream()
                .map(this::toDto)
                .toList();
    }

    public SmtpConnectionDto getOwnedConnectionDto(String uuid, String userUuid) {
        return toDto(getOwnedConnection(uuid, userUuid));
    }

    public SmtpConnectionEntity getOwnedConnection(String uuid, String userUuid) {
        return smtpConnectionRepository
                .findByUuidAndOwnerUserUuid(uuid, userUuid)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Connection not found"));
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
            String uuid, String userUuid, UpdateSmtpConnectionDto dto) {
        SmtpConnectionEntity entity = getOwnedConnection(uuid, userUuid);
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
    public void deleteConnection(String uuid, String userUuid) {
        SmtpConnectionEntity entity = getOwnedConnection(uuid, userUuid);
        apiKeyRepository.deleteAllBySmtpConnectionUuid(uuid);
        smtpConnectionRepository.delete(entity);
    }

    public SmtpConnectionDto toDto(SmtpConnectionEntity entity) {
        return SmtpConnectionDto.builder()
                .uuid(entity.getUuid())
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
