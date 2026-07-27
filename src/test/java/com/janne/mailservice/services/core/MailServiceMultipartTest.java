package com.janne.mailservice.services.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.janne.mailservice.entity.MailEntity;
import com.janne.mailservice.entity.SmtpConnectionEntity;
import com.janne.mailservice.metrics.MailMetrics;
import com.janne.mailservice.model.action.SendMailDto;
import com.janne.mailservice.repository.MailRepository;
import com.janne.mailservice.repository.SmtpConnectionRepository;
import com.janne.mailservice.services.mail.MailDispatcher;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Covers the {@code textBody} alternative: when a caller supplies both representations the message
 * must carry a plain-text and an HTML part, and when it does not the message must keep exactly the
 * shape it had before the field existed.
 */
class MailServiceMultipartTest {

    private static final String CONNECTION_UUID = "conn-1";

    private final MailRepository mailRepository = mock(MailRepository.class);
    private final SmtpConnectionRepository connectionRepository =
            mock(SmtpConnectionRepository.class);
    private final MailDispatcher mailDispatcher = mock(MailDispatcher.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);

    private final MailService service =
            new MailService(
                    mailRepository,
                    mock(SmtpConnectionService.class),
                    connectionRepository,
                    mock(SettingsService.class),
                    mailDispatcher,
                    mock(MailMetrics.class));

    @BeforeEach
    void setUp() {
        SmtpConnectionEntity connection = new SmtpConnectionEntity();
        connection.setFromAddress("noreply@example.com");
        when(connectionRepository.findById(CONNECTION_UUID)).thenReturn(Optional.of(connection));
        when(mailDispatcher.buildSender(any())).thenReturn(sender);
        when(sender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        when(mailRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private MimeMessage send(SendMailDto dto) throws Exception {
        service.sendMail(CONNECTION_UUID, "key-1", dto);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        MimeMessage message = captor.getValue();
        // The real sender calls this on transmit. Without it the Content-Type header still
        // reports the default text/plain no matter what content was set.
        message.saveChanges();
        return message;
    }

    private static SendMailDto dto(String body, boolean html, String textBody) {
        SendMailDto dto = new SendMailDto();
        dto.setRecipient("someone@example.com");
        dto.setSubject("Subject");
        dto.setBody(body);
        dto.setEnableHtml(html);
        dto.setTextBody(textBody);
        return dto;
    }

    /**
     * MimeMessageHelper nests the alternative inside its standard mixed/related container rather
     * than making it the root, so the assertion is on the leaf parts that actually reach the
     * client: one text/plain and one text/html, ordered least- to most-preferred.
     */
    @Test
    void htmlWithTextBody_isSentWithBothLeafParts() throws Exception {
        MimeMessage message = send(dto("<p>Rich</p>", true, "Plain"));

        assertThat(message.getContentType()).contains("multipart/");
        assertThat(leafContentTypes(message.getContent()))
                .containsExactly("text/plain", "text/html");
    }

    /** Depth-first walk of the MIME tree, returning the bare media type of every leaf. */
    private static List<String> leafContentTypes(Object content) throws Exception {
        if (!(content instanceof Multipart multipart)) {
            return List.of();
        }
        List<String> types = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object body = part.getContent();
            if (body instanceof Multipart) {
                types.addAll(leafContentTypes(body));
            } else {
                types.add(part.getContentType().split(";")[0].trim().toLowerCase());
            }
        }
        return types;
    }

    @Test
    void htmlWithoutTextBody_staysSinglePart() throws Exception {
        MimeMessage message = send(dto("<p>Rich</p>", true, null));

        assertThat(message.getContentType()).contains("text/html");
        assertThat(message.getContent()).isEqualTo("<p>Rich</p>");
    }

    /** A plain-text body needs no alternative, so the field is ignored rather than honoured. */
    @Test
    void plainTextBody_ignoresTextBody() throws Exception {
        MimeMessage message = send(dto("Plain", false, "Also plain"));

        assertThat(message.getContentType()).contains("text/plain");
        assertThat(message.getContent()).isEqualTo("Plain");
    }

    @Test
    void blankTextBody_isTreatedAsAbsent() throws Exception {
        MimeMessage message = send(dto("<p>Rich</p>", true, "   "));

        assertThat(message.getContentType()).contains("text/html");
    }

    /** The stored record keeps the HTML body, as it did before the alternative existed. */
    @Test
    void storedRecord_isUnchangedByTheAlternative() {
        service.sendMail(CONNECTION_UUID, "key-1", dto("<p>Rich</p>", true, "Plain"));

        ArgumentCaptor<MailEntity> captor = ArgumentCaptor.forClass(MailEntity.class);
        verify(mailRepository).save(captor.capture());
        assertThat(captor.getValue().getBody()).isEqualTo("<p>Rich</p>");
        assertThat(captor.getValue().isHtml()).isTrue();
        assertThat(captor.getValue().isSuccess()).isTrue();
    }
}
