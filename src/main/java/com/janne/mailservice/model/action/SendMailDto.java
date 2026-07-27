package com.janne.mailservice.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMailDto {

    @NotBlank @Email private String recipient;

    @NotBlank
    @Size(max = 500)
    private String subject;

    @NotBlank private String body;

    private boolean enableHtml;

    /**
     * Optional plain-text alternative, sent alongside an HTML {@code body} as a {@code
     * multipart/alternative} message. Clients that cannot or will not render HTML show this instead
     * of a blank message or a wall of stripped markup, which matters for transactional and security
     * mail where the content is the point.
     *
     * <p>Ignored unless {@code enableHtml} is set — a plain-text body needs no alternative.
     */
    private String textBody;
}
