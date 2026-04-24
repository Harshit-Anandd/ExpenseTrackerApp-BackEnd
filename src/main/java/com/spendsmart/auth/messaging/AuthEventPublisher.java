package com.spendsmart.auth.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.messaging.auth-notification-exchange}")
    private String exchange;

    @Value("${app.messaging.auth-notification-routing-key}")
    private String routingKey;

    public void publish(String eventType, Long userId, String email, String title, String message, String severity, String otpCode) {
        AuthNotificationEvent event = AuthNotificationEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .email(email)
                .title(title)
                .message(message)
                .severity(severity)
                .otpCode(otpCode)
                .occurredAt(Instant.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            // Keep auth flows resilient even when broker is unavailable.
            log.warn("Failed to publish auth event {} for user {}: {}", eventType, userId, ex.getMessage());
        }
    }
}

