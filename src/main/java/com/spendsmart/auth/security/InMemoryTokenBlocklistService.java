package com.spendsmart.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InMemoryTokenBlocklistService implements TokenBlocklistService {

	private static final String KEY_PREFIX = "auth:revoked-token:";

	private final StringRedisTemplate redisTemplate;
	private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

	public InMemoryTokenBlocklistService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
		this.redisTemplate = redisTemplateProvider.getIfAvailable();
	}

	@Override
	public void revokeToken(String token, long ttlMillis) {
		if (token == null || token.isBlank() || ttlMillis <= 0) {
			return;
		}

		if (redisTemplate != null) {
			try {
				redisTemplate.opsForValue().set(buildKey(token), "1", Duration.ofMillis(ttlMillis));
				return;
			} catch (Exception ex) {
				log.warn("Redis unavailable for token revoke, using in-memory fallback: {}", ex.getMessage());
			}
		}

		revokedTokens.put(token, System.currentTimeMillis() + ttlMillis);
	}

	@Override
	public boolean isRevoked(String token) {
		if (token == null || token.isBlank()) {
			return false;
		}

		if (redisTemplate != null) {
			try {
				Boolean exists = redisTemplate.hasKey(buildKey(token));
				return Boolean.TRUE.equals(exists);
			} catch (Exception ex) {
				log.warn("Redis unavailable for token lookup, using in-memory fallback: {}", ex.getMessage());
			}
		}

		Long expiresAt = revokedTokens.get(token);
		if (expiresAt == null) {
			return false;
		}

		if (expiresAt <= System.currentTimeMillis()) {
			revokedTokens.remove(token);
			return false;
		}

		return true;
	}

	private String buildKey(String token) {
		return KEY_PREFIX + token;
	}
}

