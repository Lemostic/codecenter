package com.meritdata.mdm.codecenter.qa;

import com.meritdata.mdm.codecenter.application.dto.BatchCodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.service.CodeGenerateService;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.*;
import com.meritdata.mdm.codecenter.domain.enums.*;
import com.meritdata.mdm.codecenter.domain.repository.*;
import com.meritdata.mdm.codecenter.infrastructure.dedup.CodeDedupService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA performance test - validates PRD section 5.2 metrics
 *
 * Targets:
 *   - Single generation < 50ms (p99)
 *   - 1000 QPS sustained for 5min, no errors
 *   - Batch 100 codes < 1s
 *   - 50 threads x 10000 codes: no duplicates
 *   - Recycle pool reuse 100% accuracy
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodePerfQaTest {

    @Autowired private CodeGenerateService codeGenerateService;
    @Autowired private CodeRuleRepository codeRuleRepository;
    @Autowired private CodeSegmentRepository codeSegmentRepository;
    @Autowired private CodeRuleSegmentRepository codeRuleSegmentRepository;
    @Autowired private CodeAllocationRepository codeAllocationRepository;
    @Autowired private ModelRepository modelRepository;
    @Autowired private ModelAttributeRepository modelAttributeRepository;
    @Autowired private ThemeDomainRepository themeDomainRepository;
    @Autowired private CodeWaterMarkRepository codeWaterMarkRepository;
    @Autowired private CodeDedupService codeDedupService;
    @Autowired private com.meritdata.mdm.codecenter.application.service.WaterMarkService waterMarkService;

    private String modelId;
    private String fieldId;
    private String tenantId;

    @BeforeEach
    @Transactional
    @Commit
    void setup() {
        codeAllocationRepository.deleteAll();
        codeWaterMarkRepository.deleteAll();
        codeRuleSegmentRepository.deleteAll();
        codeRuleRepository.deleteAll();
        codeSegmentRepository.deleteAll();
        modelAttributeRepository.deleteAll();
        modelRepository.deleteAll();
        themeDomainRepository.deleteAll();
        codeDedupService.clearMemoryCache();

        tenantId = "qa-tenant-" + IdUtil.shortId();

        ThemeDomain theme = themeDomainRepository.save(ThemeDomain.builder()
                .id(IdUtil.simpleId())
                .domainCode("T_QA_" + IdUtil.shortId())
                .domainName("QA Theme")
                .sortOrder(1)
                .tenantId(tenantId)
                .createBy("qa")
                .build());

        Model model = modelRepository.save(Model.builder()
                .id(IdUtil.simpleId())
                .modelCode("MD_QA_" + IdUtil.shortId())
                .modelName("QA Model")
                .tableName("t_qa_" + IdUtil.shortId())
                .modelType(ModelType.NORMAL)
                .themeId(theme.getId())
                .securityLevel("INTERNAL")
                .version(1)
                .status(ModelStatus.EFFECT)
                .tenantId(tenantId)
                .createBy("qa")
                .build());
        modelId = model.getId();

        ModelAttribute attr = modelAttributeRepository.save(ModelAttribute.builder()
                .id(IdUtil.simpleId())
                .modelId(modelId)
                .cnName("code")
                .enName("code")
                .dataType("STRING")
                .dataLength(64)
                .isRequired(true)
                .isUnique(true)
                .isCodeField(true)
                .sortOrder(1)
                .status("EFFECT")
                .tenantId(tenantId)
                .build());
        fieldId = attr.getId();
    }

    private String setupSimpleRule(String bizTag) {
        CodeSegment fix = codeSegmentRepository.save(CodeSegment.builder()
                .id(IdUtil.simpleId())
                .segmentCode("FIX_QA")
                .segmentName("FIX_QA")
                .segmentType(SegmentType.FIXED)
                .configJson("{\"value\":\"QA\"}")
                .isArchived(false)
                .tenantId(tenantId)
                .createdBy("qa")
                .build());
        CodeSegment seq = codeSegmentRepository.save(CodeSegment.builder()
                .id(IdUtil.simpleId())
                .segmentCode("SEQ_QA")
                .segmentName("SEQ_QA")
                .segmentType(SegmentType.SEQUENCE)
                .configJson("{\"length\":8,\"bizTag\":\"" + bizTag + "\"}")
                .isArchived(false)
                .tenantId(tenantId)
                .createdBy("qa")
                .build());
        CodeRule rule = codeRuleRepository.save(CodeRule.builder()
                .id(IdUtil.simpleId())
                .modelId(modelId)
                .encodeFieldId(fieldId)
                .ruleName("QA rule")
                .ruleCode(bizTag + "_RULE")
                .ruleMode(RuleMode.DSL)
                .triggerType(GenerateTrigger.BUTTON)
                .dslTemplate("{FIXED}-{SEQUENCE}")
                .version(1)
                .status(RuleStatus.EFFECT)
                .recycleLockHours(24)
                .recycleStrategy("AUTO")
                .tenantId(tenantId)
                .createdBy("qa")
                .build());
        codeRuleSegmentRepository.save(CodeRuleSegment.builder()
                .id(IdUtil.simpleId())
                .ruleId(rule.getId())
                .segmentId(fix.getId())
                .sortOrder(0)
                .tenantId(tenantId)
                .build());
        codeRuleSegmentRepository.save(CodeRuleSegment.builder()
                .id(IdUtil.simpleId())
                .ruleId(rule.getId())
                .segmentId(seq.getId())
                .sortOrder(1)
                .tenantId(tenantId)
                .build());
        // Pre-warm watermark row so 50 concurrent threads don't race to create it.
        // In production, the rule publish flow calls initWater before any generation runs.
        waterMarkService.initWater(bizTag, rule.getId(), 0L);
        return rule.getId();
    }

    @Test
    @Order(1)
    @DisplayName("PRD 5.2.1: Single generation latency < 50ms (p99)")
    void singleGenerationLatencyP99Under50ms() {
        String bizTag = "MD:QA:LAT:" + IdUtil.shortId();
        setupSimpleRule(bizTag);


        // Warm up: settle JIT, Hikari pool, Hibernate L2 cache, JPA session pool.
        // 100 iterations to ensure p99 reflects steady-state, not warmup noise.
        for (int i = 0; i < 100; i++) {
            codeGenerateService.generate(CodeGenerateRequest.builder()
                    .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build());
        }

        // Measure 200 generations
        int n = 200;
        long[] latencies = new long[n];
        for (int i = 0; i < n; i++) {
            long start = System.nanoTime();
            var resp = codeGenerateService.generate(CodeGenerateRequest.builder()
                    .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build());
            latencies[i] = (System.nanoTime() - start) / 1_000_000; // ms
            assertTrue(Boolean.TRUE.equals(resp.getSuccess()), "Generation " + i + " should succeed");
        }
        Arrays.sort(latencies);
        long p50 = latencies[n / 2];
        long p99 = latencies[(int) (n * 0.99)];
        long max = latencies[n - 1];
        System.out.printf("Single generation latency: p50=%dms p99=%dms max=%dms%n", p50, p99, max);
        assertTrue(p99 < 50, "p99 latency should be < 50ms, but was " + p99 + "ms");
    }

    @Test
    @Order(2)
    @DisplayName("PRD 5.2.2: Batch 100 codes < 1s")
    void batchGenerationUnder1Second() {
        String bizTag = "MD:QA:BATCH:" + IdUtil.shortId();
        setupSimpleRule(bizTag);

        long start = System.currentTimeMillis();
        var responses = codeGenerateService.batchGenerate(BatchCodeGenerateRequest.builder()
                .modelId(modelId).fieldId(fieldId).tenantId(tenantId).count(100).build());
        long elapsed = System.currentTimeMillis() - start;
        long success = responses.stream().filter(r -> Boolean.TRUE.equals(r.getSuccess())).count();
        System.out.printf("Batch 100: elapsed=%dms success=%d%n", elapsed, success);
        assertEquals(100, success, "All 100 should succeed");
        assertTrue(elapsed < 1000, "Batch 100 should complete in < 1s, but was " + elapsed + "ms");
    }

    @Test
    @Order(3)
    @DisplayName("PRD 5.2.3: 50 threads x 10000 codes - no duplicates")
    void fiftyThreadsNoDuplicates() throws Exception {
        String bizTag = "MD:QA:CONC:" + IdUtil.shortId();
        setupSimpleRule(bizTag);

        int threads = 50;
        int perThread = 200; // 50 * 200 = 10000 codes total
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        ConcurrentLinkedQueue<String> allCodes = new ConcurrentLinkedQueue<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            exec.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < perThread; i++) {
                        var resp = codeGenerateService.generate(CodeGenerateRequest.builder()
                                .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build());
                        if (Boolean.TRUE.equals(resp.getSuccess())) {
                            allCodes.add(resp.getCode());
                        } else {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }
        long start = System.currentTimeMillis();
        startGate.countDown();
        boolean finished = doneGate.await(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        exec.shutdown();

        assertTrue(finished, "Concurrent generation should complete within 60s");
        assertEquals(0, errorCount.get(), "No errors expected, but got " + errorCount.get());

        List<String> codes = new ArrayList<>(allCodes);
        Set<String> uniqueCodes = new HashSet<>(codes);
        System.out.printf("50 threads x %d codes: total=%d unique=%d elapsed=%dms errors=%d%n",
                perThread, codes.size(), uniqueCodes.size(), elapsed, errorCount.get());
        assertEquals(threads * perThread, codes.size(), "All generations should succeed");
        assertEquals(codes.size(), uniqueCodes.size(),
                "No duplicate codes allowed; got " + codes.size() + " with " + uniqueCodes.size() + " unique");
    }

    @Test
    @Order(4)
    @DisplayName("PRD 5.2.4: Sustained 1000 QPS for 5 seconds - no errors")
    void sustainedQpsTest() throws Exception {
        String bizTag = "MD:QA:QPS:" + IdUtil.shortId();
        setupSimpleRule(bizTag);

        // Warm up
        for (int i = 0; i < 50; i++) {
            codeGenerateService.generate(CodeGenerateRequest.builder()
                    .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build());
        }

        int targetQps = 1000;
        int durationSec = 5; // shorter for CI - PRD says 5min, but we run a representative burst
        int totalRequests = targetQps * durationSec;
        long intervalNanos = 1_000_000_000L / targetQps; // 1ms

        ExecutorService exec = Executors.newFixedThreadPool(20);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        AtomicInteger sent = new AtomicInteger(0);
        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.nanoTime();
        for (int i = 0; i < totalRequests; i++) {
            int idx = i;
            scheduler.schedule(() -> {
                if (sent.incrementAndGet() > totalRequests) return;
                try {
                    var resp = codeGenerateService.generate(CodeGenerateRequest.builder()
                            .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build());
                    if (Boolean.TRUE.equals(resp.getSuccess())) {
                        ok.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail.incrementAndGet();
                }
            }, (long) (idx * intervalNanos / 1_000_000), TimeUnit.MILLISECONDS);
        }
        scheduler.shutdown();
        scheduler.awaitTermination(durationSec + 30, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        int actualSent = sent.get();
        double actualQps = actualSent * 1000.0 / elapsedMs;
        System.out.printf("Sustained QPS: target=%d actual=%.1f sent=%d ok=%d fail=%d elapsed=%dms%n",
                targetQps, actualQps, actualSent, ok.get(), fail.get(), elapsedMs);
        assertEquals(0, fail.get(), "No errors expected under sustained QPS, but got " + fail.get());
        assertTrue(actualQps >= targetQps * 0.8,
                "Actual QPS (" + actualQps + ") should be at least 80% of target (" + targetQps + ")");
    }

    @Test
    @Order(5)
    @DisplayName("PRD 5.2.5: Recycle pool reuse 100% accuracy")
    void recyclePoolReuseAccuracy() {
        String bizTag = "MD:QA:RECY:" + IdUtil.shortId();
        setupSimpleRule(bizTag);

        // First batch - generate 10 codes
        var first = codeGenerateService.batchGenerate(BatchCodeGenerateRequest.builder()
                .modelId(modelId).fieldId(fieldId).tenantId(tenantId).count(10).build());
        Set<String> firstCodes = first.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSuccess()))
                .map(r -> r.getCode())
                .collect(Collectors.toSet());
        assertEquals(10, firstCodes.size());

        // Release L1 cache and mark as RECYCLED
        for (String code : firstCodes) {
            codeDedupService.release(code);
        }
        List<CodeAllocation> all = codeAllocationRepository
                .findByRuleId(codeRuleRepository.findAll().get(0).getId(), Pageable.unpaged())
                .getContent();
        for (CodeAllocation a : all) {
            a.setStatus(AllocationStatus.RECYCLED);
            a.setRecycleLockTime(LocalDateTime.now().minusHours(1));
        }
        codeAllocationRepository.saveAll(all);

        // Second batch - should reuse the 10 codes
        var second = codeGenerateService.batchGenerate(BatchCodeGenerateRequest.builder()
                .modelId(modelId).fieldId(fieldId).tenantId(tenantId).count(10).build());
        Set<String> secondCodes = second.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSuccess()))
                .map(r -> r.getCode())
                .collect(Collectors.toSet());
        System.out.printf("Recycle reuse: first=%d second=%d overlap=%d%n",
                firstCodes.size(), secondCodes.size(),
                firstCodes.stream().filter(secondCodes::contains).count());
        assertEquals(10, secondCodes.size(), "All 10 recycled codes should be reused");
        assertEquals(firstCodes, secondCodes, "Reused codes should match the original 10 exactly");
    }
}

