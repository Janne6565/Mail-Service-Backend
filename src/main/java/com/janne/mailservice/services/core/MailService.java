package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.MailEntity;
import com.janne.mailservice.entity.SmtpConnectionEntity;
import com.janne.mailservice.model.action.SendMailDto;
import com.janne.mailservice.model.core.MailDto;
import com.janne.mailservice.repository.MailRepository;
import com.janne.mailservice.repository.SmtpConnectionRepository;
import com.janne.mailservice.services.mail.MailDispatcher;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailRepository mailRepository;
    private final SmtpConnectionService smtpConnectionService;
    private final SmtpConnectionRepository smtpConnectionRepository;
    private final SettingsService settingsService;
    private final MailDispatcher mailDispatcher;

    public MailDto sendMail(String connectionUuid, String apiKeyUuid, SendMailDto dto) {
        SmtpConnectionEntity connection =
                smtpConnectionRepository
                        .findById(connectionUuid)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Connection not found"));

        boolean success = true;
        String errorMessage = null;

        try {
            JavaMailSender sender = mailDispatcher.buildSender(connection);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(connection.getFromAddress());
            helper.setTo(dto.getRecipient());
            helper.setSubject(dto.getSubject());
            helper.setText(dto.getBody(), dto.isHtml());
            sender.send(message);
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("Mail send failed for connection {}: {}", connectionUuid, errorMessage);
        }

        MailEntity entity =
                MailEntity.builder()
                        .smtpConnectionUuid(connectionUuid)
                        .apiKeyUuid(apiKeyUuid)
                        .recipient(dto.getRecipient())
                        .subject(dto.getSubject())
                        .body(dto.getBody())
                        .html(dto.isHtml())
                        .success(success)
                        .errorMessage(errorMessage)
                        .build();

        return toDto(mailRepository.save(entity));
    }

    public Page<MailDto> getMailsByApiKey(String connectionUuid, int page, int size) {
        return mailRepository
                .findAllBySmtpConnectionUuid(
                        connectionUuid,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt")))
                .map(this::toDto);
    }

    public MailDto getMailByIdForApiKey(String mailUuid, String connectionUuid) {
        return mailRepository
                .findByUuidAndSmtpConnectionUuid(mailUuid, connectionUuid)
                .map(this::toDto)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mail not found"));
    }

    public Page<MailDto> getMailsForConnection(
            String connectionUuid, String userUuid, int page, int size) {
        smtpConnectionService.getOwnedConnection(connectionUuid, userUuid);
        return mailRepository
                .findAllBySmtpConnectionUuid(
                        connectionUuid,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt")))
                .map(this::toDto);
    }

    public MailDto getMailById(String mailUuid, String connectionUuid, String userUuid) {
        smtpConnectionService.getOwnedConnection(connectionUuid, userUuid);
        return mailRepository
                .findByUuidAndSmtpConnectionUuid(mailUuid, connectionUuid)
                .map(this::toDto)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mail not found"));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public int purgeExpiredMails() {
        int retentionDays = settingsService.getSettings().getMailRetentionDays();
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = mailRepository.deleteAllBySentAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} expired mail records (retention: {} days)", deleted, retentionDays);
        }
        return deleted;
    }

    public MailDto toDto(MailEntity entity) {
        return MailDto.builder()
                .uuid(entity.getUuid())
                .smtpConnectionUuid(entity.getSmtpConnectionUuid())
                .apiKeyUuid(entity.getApiKeyUuid())
                .recipient(entity.getRecipient())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .html(entity.isHtml())
                .sentAt(entity.getSentAt())
                .success(entity.isSuccess())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
