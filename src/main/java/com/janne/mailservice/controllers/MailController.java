package com.janne.mailservice.controllers;

import com.janne.mailservice.entities.MailEntity;
import com.janne.mailservice.services.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mail")
public class MailController {

    private final MailService mailService;

    @PostMapping
    public ResponseEntity<?> sendMail(@RequestBody MailEntity mailMessage) {
        try {
            return ResponseEntity.ok(mailService.sendMail(mailMessage));
        } catch (Exception e) {
            log.error("Error while sending", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{mail-uuid}")
    public ResponseEntity<MailEntity> getMailById(@PathVariable("mail-uuid") String mailUuid) {
        return ResponseEntity.ok(mailService.getMailById(mailUuid));
    }

    @GetMapping
    public ResponseEntity<List<MailEntity>> getMails(@RequestParam int page, @RequestParam int pageSize) {
        return ResponseEntity.ok(mailService.getAllMails(page, pageSize));
    }
}
