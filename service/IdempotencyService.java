package com.meshrouteddeferredsettlement.upi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IdempotencyService {

    private final Map<String, Instant> seen = new ConcurrentHashMap<>();

    @Value("${upi.mesh.idempotency-ttl-seconds:86400}")
    private long ttlSeconds;

    /**
     * Try to claim a hash.
     * Returns true if first time, false if duplicate.
     */
    public boolean claim(String packetHash) {
        Instant now = Instant.now();
        Instant prev = seen.putIfAbsent(packetHash, now);

        if (prev == null) {
            log.debug("Claimed new packetHash: {}", packetHash.substring(0, 8));
            return true;
        } else {
            log.debug("Duplicate packetHash ignored: {}", packetHash.substring(0, 8));
            return false;
        }
    }

    public int size() {
        return seen.size();
    }

    /**
     * Remove expired entries periodically.
     */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(ttlSeconds);
        int before = seen.size();

        seen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));

        int after = seen.size();
        log.debug("Evicted {} expired entries", (before - after));
    }

    public void clear() {
        seen.clear();
    }
}