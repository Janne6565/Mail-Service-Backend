package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.CreateApiKeyDto;
import com.janne.mailservice.model.core.ApiKeyCreationResultDto;
import com.janne.mailservice.model.core.ApiKeyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "API Keys")
@RequestMapping("/v1/smtp-connections/{connectionId}/api-keys")
public interface ApiKeyApi {

    @Operation(summary = "List API keys for a connection")
    @GetMapping
    ResponseEntity<List<ApiKeyDto>> listKeys(@PathVariable String connectionId);

    @Operation(summary = "Create an API key — the secret is returned exactly once in the response")
    @ApiResponse(responseCode = "201")
    @PostMapping
    ResponseEntity<ApiKeyCreationResultDto> createKey(
            @PathVariable String connectionId, @Valid @RequestBody CreateApiKeyDto dto);

    @Operation(summary = "Revoke an API key")
    @ApiResponse(responseCode = "204")
    @DeleteMapping("/{keyId}")
    ResponseEntity<Void> revokeKey(@PathVariable String connectionId, @PathVariable String keyId);
}
