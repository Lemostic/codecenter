$file = "D:\dev\Projects\codecenter\src\main\java/com/meritdata/mdm/codecenter/infrastructure/cosid/CosIdGenerator.java"
$content = @"
package com.meritdata.mdm.codecenter.infrastructure.cosid;

import lombok.extern.slf4j.Slf4j;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import me.ahoo.cosid.segment.IdSegmentDistributor;
import me.ahoo.cosid.segment.IdSegmentDistributorDefinition;
import me.ahoo.cosid.segment.IdSegmentDistributorFactory;
import me.ahoo.cosid.segment.SegmentChainId;
import me.ahoo.cosid.segment.concurrent.PrefetchWorkerExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CosIdGenerator {

    private static final AtomicInteger INSTANCES = new AtomicInteger(0);

    private final IdGeneratorProvider idGeneratorProvider;
    private final IdSegmentDistributorFactory distributorFactory;
    private final PrefetchWorkerExecutorService prefetchWorker;
    private final String namespace;
    private final int defaultStep;

    private final ConcurrentHashMap<String, AtomicLong> localFallback = new ConcurrentHashMap<>();
    private final int instanceId;

    public CosIdGenerator(
            @Autowired(required = false) IdGeneratorProvider idGeneratorProvider,
            @Autowired(required = false) IdSegmentDistributorFactory distributorFactory,
            @Autowired(required = false) PrefetchWorkerExecutorService prefetchWorker,
            @Value("${cosid.namespace:cosid}") String namespace,
            @Value("${cosid.segment.step:100}" int defaultStep) {
        this.idGeneratorProvider = idGeneratorProvider;
        this.distributorFactory = distributorFactory;
        this.prefetchWorker = prefetchWorker;
        this.namespace = namespace;
        this.defaultStep = defaultStep;
        this.instanceId = INSTANCES.incrementAndGet();
        log.warn("CosIdGenerator ctor: instanceId={}, provider={}, factory={}, prefetch={}, namespace={}, step={}",
                instanceId,
                idGeneratorProvider != null ? "ok" : "fallback",
                distributorFactory != null ? "ok" : "fallback",
                prefetchWorker != null ? "ok" : "fallback",
                namespace, defaultStep);
    }

    public long nextSequence(String bizTag) {
        if (idGeneratorProvider == null) {
            AtomicLong al = localFallback.computeIfAbsent(bizTag, k -> new AtomicLong(0));
            return al.incrementAndGet();
        }
        IdGenerator idGenerator = idGeneratorProvider.get(bizTag)
                .orElseGet(() -> createAndRegister(bizTag));
        return idGenerator.generate();
    }

    public List<Long> nextSequenceBatch(String bizTag, int count) {
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(nextSequence(bizTag));
        }
        return ids;
    }

    public void ensureBizTypeInitialized(String bizTag) {
        if (idGeneratorProvider != null && !idGeneratorProvider.get(bizTag).isPresent()) {
            createAndRegister(bizTag);
        }
    }

    private IdGenerator createAndRegister(String bizTag) {
        if (distributorFactory == null || prefetchWorker == null) {
            return null;
        }
        IdSegmentDistributorDefinition definition = new IdSegmentDistributorDefinition(
                namespace, bizTag, IdSegmentDistributor.DEFAULT_OFFSET, defaultStep);
        IdSegmentDistributor distributor = distributorFactory.create(definition);
        SegmentChainId chainId = new SegmentChainId(distributor);
        idGeneratorProvider.set(bizTag, chainId);
        log.info("CosId auto-created IdGenerator: bizTag={}, namespace={}, step={}", bizTag, namespace, defaultStep);
        return chainId;
    }
}
"@
Set-Content $file $content -Encoding UTF8
Write-Host "Reverted"
