package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.MailApi;
import com.janne.mailservice.entity.ApiKeyScope;
import com.janne.mailservice.model.core.MailDto;
import com.janne.mailservice.security.apikeyfilter.ApiKeyAuthenticationToken;
import com.janne.mailservice.services.auth.SecurityContextService;
import com.janne.mailservice.services.core.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class MailController implements MailApi {

    private final MailService mailService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<Page<MailDto>> listMails(int page, int size) {
        ApiKeyAuthenticationToken apiKey = requireReadMailsScope();
        return ResponseEntity.ok(
                mailService.getMailsByApiKey(apiKey.getSmtpConnectionUuid(), page, size));
    }

    @Override
    public ResponseEntity<MailDto> getMail(String id) {
        ApiKeyAuthenticationToken apiKey = requireReadMailsScope();
        return ResponseEntity.ok(
                mailService.getMailByIdForApiKey(id, apiKey.getSmtpConnectionUuid()));
    }

    private ApiKeyAuthenticationToken requireReadMailsScope() {
        ApiKeyAuthenticationToken apiKey = securityContextService.requireApiKey();
        if (!apiKey.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_" + ApiKeyScope.READ_MAILS.name()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing READ_MAILS scope");
        }
        return apiKey;
    }
}
