package com.spendsmart.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisOtpChallengeService implements OtpChallengeService {

    private static final long OTP_TTL_SECONDS = 300;
    private static final String OTP_KEY_PREFIX = "auth:otp:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public OtpChallenge createChallenge(String email, OtpPurpose purpose) {
        String challengeId = UUID.randomUUID().toString();
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));

        String payload = email + "|" + purpose.name() + "|" + otpCode;
        redisTemplate.opsForValue().set(buildKey(challengeId), payload, Duration.ofSeconds(OTP_TTL_SECONDS));

        // Email dispatch will be moved to RabbitMQ worker; keeping log-based visibility for now.
        log.info("OTP challenge generated. purpose={}, email={}, challengeId={}, otpCode={}", purpose.name(), email, challengeId, otpCode);
        return new OtpChallenge(challengeId, OTP_TTL_SECONDS, otpCode);
    }

    @Override
    public boolean verify(String email, OtpPurpose purpose, String challengeId, String otpCode) {
        String key = buildKey(challengeId);
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null || payload.isBlank()) {
            return false;
        }

        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) {
            return false;
        }

        boolean matches = email.equalsIgnoreCase(parts[0])
                && purpose.name().equals(parts[1])
                && otpCode.equals(parts[2]);

        if (matches) {
            redisTemplate.delete(key);
        }
        return matches;
    }

    private String buildKey(String challengeId) {
        return OTP_KEY_PREFIX + challengeId;
    }
}

