package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.SendApi;
import com.janne.mailservice.entity.ApiKeyScope;
import com.janne.mailservice.model.action.SendMailDto;
import com.janne.mailservice.model.core.MailDto;
import com.janne.mailservice.security.apikeyfilter.ApiKeyAuthenticationToken;
import com.janne.mailservice.services.auth.SecurityContextService;
import com.janne.mailservice.services.core.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class SendController implements SendApi {

    private final MailService mailService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<MailDto> sendMail(SendMailDto dto) {
        ApiKeyAuthenticationToken apiKey = securityContextService.requireApiKey();
        if (!apiKey.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_" + ApiKeyScope.SEND.name()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing SEND scope");
        }
        return ResponseEntity.ok(
                mailService.sendMail(apiKey.getSmtpConnectionUuid(), apiKey.getPrincipal(), dto));
    }
}
