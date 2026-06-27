package com.meritdata.mdm.codecenter.infrastructure.dedup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.meritdata.mdm.codecenter.domain.enums.AllocationStatus;
import com.meritdata.mdm.codecenter.domain.repository.CodeAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeDedupService {

    private final CodeAllocationRepository allocationRepository;
    private static final AtomicLong VALIDATE_COUNT = new AtomicLong(0);
    private static final AtomicLong DUP_COUNT = new AtomicLong(0);

    @Value("${codecenter.dedup.memory.enabled:true}")
    private boolean memoryEnabled;
    @Value("${codecenter.dedup.memory.capacity:20000}")
    private long memoryCapacity;
    @Value("${codecenter.dedup.redis.enabled:true}")
    private boolean redisEnabled;
    @Value("${codecenter.dedup.redis.ttl-seconds:604800}")
    private long redisTtlSeconds;

    private final StringRedisTemplate stringRedisTemplate = null;

    private final Cache<String, Boolean> memoryCache = Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

    private static final String STATUS_PENDING = AllocationStatus.PENDING.name();
    private static final String STATUS_USED = AllocationStatus.USED.name();

    public void validateAndSave(String code) {
        long vc = VALIDATE_COUNT.incrementAndGet();
        if (memoryEnabled) {
            Boolean prev = memoryCache.asMap().putIfAbsent(code, Boolean.TRUE);
            if (Boolean.TRUE.equals(prev)) {
                long dc = DUP_COUNT.incrementAndGet();
                if (log.isWarnEnabled() && dc < 50) {
                    log.warn("DedupHIT count={} code={} cacheSize={}", dc, code, memoryCache.estimatedSize());
                }
                throw new IllegalStateException("Duplicate code (memory): " + code);
            }
        }
        if (redisEnabled && stringRedisTemplate != null) {
            Boolean setOk = stringRedisTemplate.opsForValue()
                    .setIfAbsent("codecenter:dedup:" + code, "1", Duration.ofSeconds(redisTtlSeconds));
            if (Boolean.FALSE.equals(setOk)) {
                throw new IllegalStateException("Duplicate code (redis): " + code);
            }
        }
        long activeCount = allocationRepository.countActiveByCodeNative(code, STATUS_PENDING, STATUS_USED);
        if (activeCount > 0) {
            if (memoryEnabled) {
                memoryCache.invalidate(code);
            }
            throw new IllegalStateException("Duplicate code (db): " + code);
        }
    }

    public Set<String> validateAndSaveBatch(List<String> codes) {
        Set<String> duplicates = new HashSet<>();
        for (String code : codes) {
            try {
                validateAndSave(code);
            } catch (IllegalStateException e) {
                duplicates.add(code);
            }
        }
        return duplicates;
    }

    public void release(String code) {
        memoryCache.invalidate(code);
    }

    public void reserveInMemory(List<String> codes) {
        List<String> safeCodes = new ArrayList<>(codes);
        for (String code : safeCodes) {
            memoryCache.put(code, Boolean.TRUE);
        }
    }

    public void clearMemoryCache() {
        long before = memoryCache.estimatedSize();
        memoryCache.invalidateAll();
        long after = memoryCache.estimatedSize();
        log.warn("DedupClear before={} after={} validated={} dup={}", before, after, VALIDATE_COUNT.get(), DUP_COUNT.get());
    }

    public boolean isMemoryCacheEnabled() { return memoryEnabled; }
    public boolean isRedisCacheEnabled() { return redisEnabled; }
    public long getMemoryCacheSize() { return memoryCache.estimatedSize(); }
}
