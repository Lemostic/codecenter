package com.meritdata.mdm.codecenter.infrastructure.cosid;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CosIdGeneratorTest {

    @Test
    void fallbackProducesMonotonicSequence() {
        CosIdGenerator g = new CosIdGenerator(null, null, null, "test", 100);
        long first = g.nextSequence("MD:WL:CODE");
        long second = g.nextSequence("MD:WL:CODE");
        long third = g.nextSequence("MD:WL:CODE");
        assertEquals(first + 1, second);
        assertEquals(second + 1, third);
    }

    @Test
    void fallbackIndependentPerBizTag() {
        CosIdGenerator g = new CosIdGenerator(null, null, null, "test", 100);
        long a = g.nextSequence("MD:A");
        long b = g.nextSequence("MD:B");
        long a2 = g.nextSequence("MD:A");
        assertEquals(1L, a);
        assertEquals(1L, b);
        assertEquals(2L, a2);
    }

    @Test
    void batchReturnsCorrectCount() {
        CosIdGenerator g = new CosIdGenerator(null, null, null, "test", 100);
        List<Long> batch = g.nextSequenceBatch("MD:WL", 50);
        assertEquals(50, batch.size());
        // All unique
        Set<Long> uniq = new HashSet<>(batch);
        assertEquals(50, uniq.size());
    }

    @Test
    void ensureBizTypeInitializedDoesNotThrow() {
        CosIdGenerator g = new CosIdGenerator(null, null, null, "test", 100);
        assertDoesNotThrow(() -> g.ensureBizTypeInitialized("MD:WL"));
    }

    @Test
    void threadSafetyUnderFallback() throws InterruptedException {
        CosIdGenerator g = new CosIdGenerator(null, null, null, "test", 100);
        int threads = 8;
        int perThread = 1000;
        java.util.concurrent.ConcurrentLinkedQueue<Long> all = new java.util.concurrent.ConcurrentLinkedQueue<>();
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    all.add(g.nextSequence("MD:CONCURRENT"));
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) t.join();

        assertEquals(threads * perThread, all.size());
        // Uniqueness check
        Set<Long> uniq = new HashSet<>(all);
        assertEquals(threads * perThread, uniq.size(), "All sequences must be unique under concurrency");
    }
}
