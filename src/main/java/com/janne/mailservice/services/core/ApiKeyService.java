package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.ApiKeyEntity;
import com.janne.mailservice.entity.ApiKeyScope;
import com.janne.mailservice.model.action.CreateApiKeyDto;
import com.janne.mailservice.model.core.ApiKeyCreationResultDto;
import com.janne.mailservice.model.core.ApiKeyDto;
import com.janne.mailservice.repository.ApiKeyRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final int SECRET_BYTES = 32;
    private final ApiKeyRepository apiKeyRepository;
    private final SmtpConnectionService smtpConnectionService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<ApiKeyDto> getKeysForConnection(String connectionUuid, String userUuid) {
        smtpConnectionService.getOwnedConnection(connectionUuid, userUuid);
        return apiKeyRepository.findAllBySmtpConnectionUuid(connectionUuid).stream()
                .map(this::toDto)
                .toList();
    }

    public ApiKeyCreationResultDto createApiKey(
            String connectionUuid, String userUuid, CreateApiKeyDto dto) {
        smtpConnectionService.getOwnedConnection(connectionUuid, userUuid);

        byte[] secretBytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secretBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        Set<ApiKeyScope> scopes = new HashSet<>();
        scopes.add(ApiKeyScope.SEND);
        if (dto.isEnableReadMails()) {
            scopes.add(ApiKeyScope.READ_MAILS);
        }

        ApiKeyEntity entity =
                ApiKeyEntity.builder()
                        .smtpConnectionUuid(connectionUuid)
                        .label(dto.getLabel())
                        .secretHash(passwordEncoder.encode(secret))
                        .scopes(scopes)
                        .build();
        ApiKeyEntity saved = apiKeyRepository.save(entity);

        return ApiKeyCreationResultDto.builder()
                .uuid(saved.getUuid())
                .label(saved.getLabel())
                .scopes(saved.getScopes())
                .createdAt(saved.getCreatedAt())
                .apiKey("mk_" + saved.getUuid() + "_" + secret)
                .build();
    }

    public void revokeApiKey(String keyUuid, String connectionUuid, String userUuid) {
        smtpConnectionService.getOwnedConnection(connectionUuid, userUuid);
        ApiKeyEntity key =
                apiKeyRepository
                        .findById(keyUuid)
                        .filter(k -> k.getSmtpConnectionUuid().equals(connectionUuid))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "API key not found"));
        key.setRevokedAt(Instant.now());
        apiKeyRepository.save(key);
    }

    public ApiKeyEntity getKeyById(String uuid) {
        return apiKeyRepository
                .findById(uuid)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "API key not found"));
    }

    public ApiKeyDto toDto(ApiKeyEntity key) {
        return ApiKeyDto.builder()
                .uuid(key.getUuid())
                .label(key.getLabel())
                .scopes(key.getScopes())
                .createdAt(key.getCreatedAt())
                .lastUsedAt(key.getLastUsedAt())
                .revoked(key.isRevoked())
                .build();
    }
}
