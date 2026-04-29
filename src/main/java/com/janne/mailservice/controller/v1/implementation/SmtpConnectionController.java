package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.SmtpConnectionApi;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.CreateSmtpConnectionDto;
import com.janne.mailservice.model.action.GrantAccessDto;
import com.janne.mailservice.model.action.SendMailDto;
import com.janne.mailservice.model.action.UpdateSmtpConnectionDto;
import com.janne.mailservice.model.core.ConnectionAccessDto;
import com.janne.mailservice.model.core.MailDto;
import com.janne.mailservice.model.core.SmtpConnectionDto;
import com.janne.mailservice.services.auth.SecurityContextService;
import com.janne.mailservice.services.core.MailService;
import com.janne.mailservice.services.core.SmtpConnectionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SmtpConnectionController implements SmtpConnectionApi {

    private final SmtpConnectionService smtpConnectionService;
    private final MailService mailService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<List<SmtpConnectionDto>> listConnections() {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(smtpConnectionService.getConnectionsForUser(user));
    }

    @Override
    public ResponseEntity<SmtpConnectionDto> createConnection(CreateSmtpConnectionDto dto) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(smtpConnectionService.createConnection(user.getUuid(), dto));
    }

    @Override
    public ResponseEntity<SmtpConnectionDto> getConnection(String id) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(smtpConnectionService.getConnectionDto(id, user));
    }

    @Override
    public ResponseEntity<SmtpConnectionDto> updateConnection(
            String id, UpdateSmtpConnectionDto dto) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(smtpConnectionService.updateConnection(id, user, dto));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(String id) {
        UserEntity user = securityContextService.requireUser();
        smtpConnectionService.deleteConnection(id, user);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Page<MailDto>> getMails(String id, int page, int size, String apiKeyUuid) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(mailService.getMailsForConnection(id, user, page, size, apiKeyUuid));
    }

    @Override
    public ResponseEntity<MailDto> sendMail(String id, SendMailDto dto) {
        UserEntity user = securityContextService.requireUser();
        smtpConnectionService.requireAccess(id, user);
        return ResponseEntity.ok(mailService.sendMail(id, null, dto));
    }

    @Override
    public ResponseEntity<Void> testConnection(String id) {
        UserEntity user = securityContextService.requireUser();
        smtpConnectionService.testConnection(id, user);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<ConnectionAccessDto>> listAccess(String id) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(smtpConnectionService.listAccess(id, user));
    }

    @Override
    public ResponseEntity<ConnectionAccessDto> grantAccess(String id, GrantAccessDto dto) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(smtpConnectionService.grantAccess(id, user, dto));
    }

    @Override
    public ResponseEntity<Void> revokeAccess(String id, String userUuid) {
        UserEntity user = securityContextService.requireUser();
        smtpConnectionService.revokeAccess(id, user, userUuid);
        return ResponseEntity.noContent().build();
    }
}
