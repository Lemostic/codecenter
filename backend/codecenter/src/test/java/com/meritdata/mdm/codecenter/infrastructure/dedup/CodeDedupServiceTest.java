package com.meritdata.mdm.codecenter.infrastructure.dedup;

import com.meritdata.mdm.codecenter.domain.repository.CodeAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CodeDedupServiceTest {

    private CodeAllocationRepository repo;
    private CodeDedupService service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(CodeAllocationRepository.class);
        // No active duplicate by default
        Mockito.when(repo.countActiveByCodeNative(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(0L);
        Mockito.when(repo.existsByCode(Mockito.anyString())).thenReturn(false);

        service = new CodeDedupService(repo);
        ReflectionTestUtils.setField(service, "memoryEnabled", true);
        ReflectionTestUtils.setField(service, "redisEnabled", false);
    }

    @Test
    void newCodePasses() {
        assertDoesNotThrow(() -> service.validateAndSave("WL-001"));
    }

    @Test
    void duplicateInMemoryThrows() {
        service.validateAndSave("WL-001");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.validateAndSave("WL-001"));
        assertTrue(ex.getMessage().contains("Duplicate code (memory)"));
    }

    @Test
    void duplicateInDbThrows() {
        Mockito.when(repo.countActiveByCodeNative("WL-DB", "PENDING", "USED")).thenReturn(1L);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.validateAndSave("WL-DB"));
        assertTrue(ex.getMessage().contains("Duplicate code (db)"));
    }

    @Test
    void batchReturnsDuplicates() {
        service.validateAndSave("A");
        service.validateAndSave("B");
        Set<String> dup = service.validateAndSaveBatch(List.of("A", "C", "B", "D"));
        assertEquals(2, dup.size());
        assertTrue(dup.contains("A"));
        assertTrue(dup.contains("B"));
    }

    @Test
    void releaseRemovesFromMemory() {
        service.validateAndSave("X");
        service.release("X");
        assertDoesNotThrow(() -> service.validateAndSave("X"));
    }

    @Test
    void reserveInMemoryBlocksValidation() {
        service.reserveInMemory(List.of("Y", "Z"));
        assertThrows(IllegalStateException.class, () -> service.validateAndSave("Y"));
        assertThrows(IllegalStateException.class, () -> service.validateAndSave("Z"));
    }
}
