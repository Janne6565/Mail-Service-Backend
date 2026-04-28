package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.ApiKeyApi;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.CreateApiKeyDto;
import com.janne.mailservice.model.core.ApiKeyCreationResultDto;
import com.janne.mailservice.model.core.ApiKeyDto;
import com.janne.mailservice.services.auth.SecurityContextService;
import com.janne.mailservice.services.core.ApiKeyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiKeyController implements ApiKeyApi {

    private final ApiKeyService apiKeyService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<List<ApiKeyDto>> listKeys(String connectionId) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(apiKeyService.getKeysForConnection(connectionId, user.getUuid()));
    }

    @Override
    public ResponseEntity<ApiKeyCreationResultDto> createKey(
            String connectionId, CreateApiKeyDto dto) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createApiKey(connectionId, user.getUuid(), dto));
    }

    @Override
    public ResponseEntity<Void> revokeKey(String connectionId, String keyId) {
        UserEntity user = securityContextService.requireUser();
        apiKeyService.revokeApiKey(keyId, connectionId, user.getUuid());
        return ResponseEntity.noContent().build();
    }
}
