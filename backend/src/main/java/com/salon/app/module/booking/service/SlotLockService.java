package com.salon.app.module.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotLockService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.slot-lock-ttl-seconds:600}")
    private long slotLockTtlSeconds;

    private static final String LOCK_KEY_PREFIX = "slot_lock:";

    public boolean tryLock(UUID outletId, LocalDate date, LocalTime time, UUID staffId, String sessionId) {
        String key = buildKey(outletId, date, time, staffId);
        log.info("Attempting slot lock: {}", key);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, sessionId, Duration.ofSeconds(slotLockTtlSeconds));
        boolean result = Boolean.TRUE.equals(acquired);
        log.info("Slot lock {} for key: {}", result ? "acquired" : "failed", key);
        return result;
    }

    public void releaseLock(UUID outletId, LocalDate date, LocalTime time, UUID staffId) {
        String key = buildKey(outletId, date, time, staffId);
        log.info("Releasing slot lock: {}", key);
        redisTemplate.delete(key);
    }

    public void extendLock(UUID outletId, LocalDate date, LocalTime time, UUID staffId) {
        String key = buildKey(outletId, date, time, staffId);
        redisTemplate.expire(key, Duration.ofSeconds(slotLockTtlSeconds));
    }

    public boolean isLocked(UUID outletId, LocalDate date, LocalTime time, UUID staffId) {
        String key = buildKey(outletId, date, time, staffId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildKey(UUID outletId, LocalDate date, LocalTime time, UUID staffId) {
        return LOCK_KEY_PREFIX + outletId + ":" + date + ":" + time + ":" + staffId;
    }
}
