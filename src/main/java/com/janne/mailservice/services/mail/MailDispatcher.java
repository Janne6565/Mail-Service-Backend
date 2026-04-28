package com.janne.mailservice.services.mail;

import com.janne.mailservice.entity.SmtpConnectionEntity;
import com.janne.mailservice.services.crypto.SmtpPasswordCipher;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailDispatcher {

    private final SmtpPasswordCipher cipher;

    public JavaMailSender buildSender(SmtpConnectionEntity connection) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(connection.getHost());
        sender.setPort(connection.getPort());
        sender.setUsername(connection.getUsername());
        sender.setPassword(cipher.decrypt(connection.getPasswordCiphertext()));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (connection.isUseStartTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");

        return sender;
    }
}
