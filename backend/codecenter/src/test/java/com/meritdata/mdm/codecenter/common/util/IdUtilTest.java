package com.meritdata.mdm.codecenter.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdUtilTest {

    @Test
    void simpleIdIs32Hex() {
        String id = IdUtil.simpleId();
        assertEquals(32, id.length());
        assertTrue(id.matches("[0-9a-f]{32}"));
    }

    @Test
    void simpleIdIsUnique() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdUtil.simpleId());
        }
        assertEquals(1000, ids.size());
    }

    @Test
    void shortIdLength() {
        assertEquals(16, IdUtil.shortId().length());
    }

    @Test
    void padSequence() {
        assertEquals("000001", IdUtil.padSequence(1, 6));
        assertEquals("000123", IdUtil.padSequence(123, 6));
        assertEquals("123456", IdUtil.padSequence(123456, 6));
        assertEquals("1234567", IdUtil.padSequence(1234567, 6));
    }

    @Test
    void padLeftAlreadyLonger() {
        assertEquals("ABC", IdUtil.padLeft("ABC", 3, '0'));
    }

    @Test
    void padLeftShorter() {
        assertEquals("00ABC", IdUtil.padLeft("ABC", 5, '0'));
    }

    @Test
    void padLeftNull() {
        assertEquals("0000", IdUtil.padLeft(null, 4, '0'));
    }
}
