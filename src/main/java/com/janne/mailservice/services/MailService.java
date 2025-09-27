package com.janne.mailservice.services;

import com.janne.mailservice.entities.MailEntity;
import com.janne.mailservice.repositories.MailRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final MailRepository mailRepository;
    @Value("${spring.mail.username}")
    private String mailAuthor;

    public MailEntity sendMail(MailEntity mailEntity) {
        mailEntity.setId(null);
        if (mailEntity.getRecipient() == null || !mailEntity.getRecipient().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address");
        }
        MailEntity sendMail = mailEntity.getEnableHtml() != null && mailEntity.getEnableHtml() ? sendHtmlEmail(mailEntity) : sendPlainTextEmail(mailEntity);
        mailRepository.save(sendMail);
        log.debug("Mail sent successfully");
        return mailEntity;
    }

    private MailEntity sendPlainTextEmail(MailEntity mailEntity) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        try {
            mailMessage.setTo(mailEntity.getRecipient());
            mailMessage.setSubject(mailEntity.getSubject());
            mailMessage.setText(mailEntity.getBody());
            mailMessage.setFrom(mailAuthor);
            mailSender.send(mailMessage);
            mailEntity.setSentDate(LocalDateTime.now());
            log.debug("Plain text Mail sent successfully");
            return mailEntity;
        } catch (Exception e) {
            log.error("Failed to send plain text email", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email");
        }
    }

    @SneakyThrows
    private MailEntity sendHtmlEmail(MailEntity mailEntity) {
        MimeMessageHelper helper = new MimeMessageHelper(mailSender.createMimeMessage(), true, "UTF-8");
        try {
            helper.setTo(mailEntity.getRecipient());
            helper.setSubject(mailEntity.getSubject());
            helper.setText(mailEntity.getBody(), true);
            helper.setFrom(mailAuthor);
            mailSender.send(helper.getMimeMessage());
            mailEntity.setSentDate(LocalDateTime.now());
            log.debug("HTML Mail sent successfully");
            return mailEntity;
        } catch (Exception e) {
            log.error("Failed to send HTML email", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email");
        }
    }

    public List<MailEntity> getAllMails(int page, int pageSize) {
        return mailRepository.findAll().subList((int) Math.min((long) (page - 1) * pageSize, mailRepository.count()), Math.min(page * pageSize, (int) mailRepository.count()));
    }

    public MailEntity getMailById(String id) {
        return mailRepository.getReferenceById(id);
    }
}
