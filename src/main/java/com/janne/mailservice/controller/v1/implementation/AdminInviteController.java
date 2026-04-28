package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.AdminInviteApi;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.CreateInviteDto;
import com.janne.mailservice.model.core.InviteDto;
import com.janne.mailservice.services.auth.SecurityContextService;
import com.janne.mailservice.services.core.InviteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminInviteController implements AdminInviteApi {

    private final InviteService inviteService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<List<InviteDto>> listInvites() {
        return ResponseEntity.ok(
                inviteService.adminListInvites().stream().map(inviteService::toDto).toList());
    }

    @Override
    public ResponseEntity<InviteDto> createInvite(CreateInviteDto dto) {
        UserEntity admin = securityContextService.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inviteService.toDto(inviteService.adminCreateInvite(admin.getUuid(), dto)));
    }

    @Override
    public ResponseEntity<Void> deleteInvite(String id) {
        inviteService.adminDeleteInvite(id);
        return ResponseEntity.noContent().build();
    }
}
