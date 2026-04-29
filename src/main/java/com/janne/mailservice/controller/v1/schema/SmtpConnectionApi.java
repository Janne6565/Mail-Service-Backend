package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.CreateSmtpConnectionDto;
import com.janne.mailservice.model.action.GrantAccessDto;
import com.janne.mailservice.model.action.SendMailDto;
import com.janne.mailservice.model.action.UpdateSmtpConnectionDto;
import com.janne.mailservice.model.core.ConnectionAccessDto;
import com.janne.mailservice.model.core.MailDto;
import com.janne.mailservice.model.core.SmtpConnectionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "SMTP Connections")
@RequestMapping("/v1/smtp-connections")
public interface SmtpConnectionApi {

    @Operation(summary = "List accessible SMTP connections (owned + granted + all for admin)")
    @GetMapping
    ResponseEntity<List<SmtpConnectionDto>> listConnections();

    @Operation(summary = "Create a new SMTP connection")
    @ApiResponse(responseCode = "201", description = "Connection created")
    @PostMapping
    ResponseEntity<SmtpConnectionDto> createConnection(
            @Valid @RequestBody CreateSmtpConnectionDto dto);

    @Operation(summary = "Get a single SMTP connection")
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", description = "Not found or no access")
    })
    @GetMapping("/{id}")
    ResponseEntity<SmtpConnectionDto> getConnection(@PathVariable String id);

    @Operation(summary = "Update an SMTP connection (owner or admin only)")
    @PatchMapping("/{id}")
    ResponseEntity<SmtpConnectionDto> updateConnection(
            @PathVariable String id, @Valid @RequestBody UpdateSmtpConnectionDto dto);

    @Operation(summary = "Delete an SMTP connection and all its API keys (owner or admin only)")
    @ApiResponse(responseCode = "204")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteConnection(@PathVariable String id);

    @Operation(summary = "List mails sent through a connection (newest first)")
    @GetMapping("/{id}/mails")
    ResponseEntity<Page<MailDto>> getMails(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "Send a mail through a connection (JWT auth)")
    @ApiResponse(responseCode = "200")
    @PostMapping("/{id}/send")
    ResponseEntity<MailDto> sendMail(
            @PathVariable String id, @Valid @RequestBody SendMailDto dto);

    @Operation(summary = "Test SMTP connectivity for a connection")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Connection OK"),
        @ApiResponse(responseCode = "502", description = "SMTP connection failed")
    })
    @PostMapping("/{id}/test")
    ResponseEntity<Void> testConnection(@PathVariable String id);

    // ── Access management (owner or admin only) ───────────────────────────────

    @Operation(summary = "List users with access to a connection")
    @GetMapping("/{id}/access")
    ResponseEntity<List<ConnectionAccessDto>> listAccess(@PathVariable String id);

    @Operation(summary = "Grant a user access to a connection")
    @ApiResponse(responseCode = "201")
    @PostMapping("/{id}/access")
    ResponseEntity<ConnectionAccessDto> grantAccess(
            @PathVariable String id, @Valid @RequestBody GrantAccessDto dto);

    @Operation(summary = "Revoke a user's access to a connection")
    @ApiResponse(responseCode = "204")
    @DeleteMapping("/{id}/access/{userUuid}")
    ResponseEntity<Void> revokeAccess(@PathVariable String id, @PathVariable String userUuid);
}
