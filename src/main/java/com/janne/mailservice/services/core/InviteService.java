package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.InviteEntity;
import com.janne.mailservice.model.action.CreateInviteDto;
import com.janne.mailservice.model.core.InviteDto;
import com.janne.mailservice.model.core.InvitePreviewDto;
import com.janne.mailservice.repository.InviteRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InviteService {

    private static final int TOKEN_BYTES = 32;
    private final InviteRepository inviteRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public InviteEntity adminCreateInvite(String creatorUuid, CreateInviteDto dto) {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        InviteEntity invite =
                InviteEntity.builder()
                        .token(token)
                        .createdByUserUuid(creatorUuid)
                        .expiresAt(Instant.now().plus(dto.getExpiresInDays(), ChronoUnit.DAYS))
                        .build();
        return inviteRepository.save(invite);
    }

    public List<InviteEntity> adminListInvites() {
        return inviteRepository.findAll();
    }

    public void adminDeleteInvite(String inviteUuid) {
        if (!inviteRepository.existsById(inviteUuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found");
        }
        inviteRepository.deleteById(inviteUuid);
    }

    public InvitePreviewDto previewInvite(String token) {
        return inviteRepository
                .findByToken(token)
                .map(
                        invite -> {
                            if (invite.getUsedAt() != null) {
                                return InvitePreviewDto.builder()
                                        .valid(false)
                                        .reason("Invite has already been used")
                                        .build();
                            }
                            if (invite.getExpiresAt().isBefore(Instant.now())) {
                                return InvitePreviewDto.builder()
                                        .valid(false)
                                        .reason("Invite has expired")
                                        .build();
                            }
                            return InvitePreviewDto.builder()
                                    .valid(true)
                                    .expiresAt(invite.getExpiresAt())
                                    .build();
                        })
                .orElse(InvitePreviewDto.builder().valid(false).reason("Invite not found").build());
    }

    public InviteEntity validateAndConsume(String token, String userUuid) {
        InviteEntity invite =
                inviteRepository
                        .findByToken(token)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Invalid invite token"));

        if (invite.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invite has already been used");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invite has expired");
        }

        invite.setUsedByUserUuid(userUuid);
        invite.setUsedAt(Instant.now());
        return inviteRepository.save(invite);
    }

    public InviteDto toDto(InviteEntity invite) {
        return InviteDto.builder()
                .uuid(invite.getUuid())
                .token(invite.getToken())
                .createdByUserUuid(invite.getCreatedByUserUuid())
                .createdAt(invite.getCreatedAt())
                .expiresAt(invite.getExpiresAt())
                .usedAt(invite.getUsedAt())
                .used(invite.getUsedAt() != null)
                .expired(
                        invite.getUsedAt() == null && invite.getExpiresAt().isBefore(Instant.now()))
                .build();
    }
}
