package com.janne.mailservice.metrics;

import com.janne.mailservice.repository.SmtpConnectionRepository;
import com.janne.mailservice.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MailMetrics {

    private final Counter mailsSentCounter;
    private final Counter mailsFailedCounter;

    public MailMetrics(
            MeterRegistry registry,
            SmtpConnectionRepository smtpConnectionRepository,
            UserRepository userRepository) {

        mailsSentCounter =
                Counter.builder("mail.send.attempts")
                        .tag("success", "true")
                        .description("Total number of emails successfully sent")
                        .register(registry);

        mailsFailedCounter =
                Counter.builder("mail.send.attempts")
                        .tag("success", "false")
                        .description("Total number of emails that failed to send")
                        .register(registry);

        Gauge.builder("smtp.connections", smtpConnectionRepository, r -> (double) r.count())
                .description("Current number of registered SMTP connections")
                .register(registry);

        Gauge.builder("users", userRepository, r -> (double) r.count())
                .description("Current number of registered users")
                .register(registry);
    }

    public void recordMailSent() {
        mailsSentCounter.increment();
    }

    public void recordMailFailed() {
        mailsFailedCounter.increment();
    }
}
