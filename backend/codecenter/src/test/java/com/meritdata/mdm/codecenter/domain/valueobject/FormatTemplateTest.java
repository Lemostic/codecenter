package com.meritdata.mdm.codecenter.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatTemplateTest {

    @Test
    void parseEmptyTemplate() {
        FormatTemplate t = FormatTemplate.parse("");
        assertNotNull(t);
        assertEquals(0, t.size());
        assertFalse(t.containsSequence());
        assertFalse(t.containsDate());
    }

    @Test
    void parseNullTemplate() {
        FormatTemplate t = FormatTemplate.parse(null);
        assertEquals(0, t.size());
    }

    @Test
    void parseLiteralOnly() {
        FormatTemplate t = FormatTemplate.parse("WL-CODE");
        assertEquals(1, t.size());
        assertEquals(FormatTemplate.TokenType.LITERAL, t.getTokens().get(0).getType());
        assertEquals("WL-CODE", t.getTokens().get(0).getLiteral());
    }

    @Test
    void parseSingleFixedSegment() {
        FormatTemplate t = FormatTemplate.parse("{FIXED:value=PR}");
        assertEquals(1, t.size());
        FormatTemplate.Token token = t.getTokens().get(0);
        assertEquals(FormatTemplate.TokenType.FIXED, token.getType());
        assertEquals("PR", token.getParams().get("value"));
    }

    @Test
    void parseDateWithFormat() {
        FormatTemplate t = FormatTemplate.parse("{DATE:format=yyyyMMdd}");
        assertEquals(1, t.size());
        assertEquals(FormatTemplate.TokenType.DATE, t.getTokens().get(0).getType());
        assertEquals("yyyyMMdd", t.getTokens().get(0).getParams().get("format"));
        assertTrue(t.containsDate());
    }

    @Test
    void parseSequenceWithMultipleParams() {
        FormatTemplate t = FormatTemplate.parse("{SEQUENCE:length=6;bizTag=MD:WL}");
        FormatTemplate.Token token = t.getTokens().get(0);
        assertEquals(FormatTemplate.TokenType.SEQUENCE, token.getType());
        assertEquals("6", token.getParams().get("length"));
        assertEquals("MD:WL", token.getParams().get("bizTag"));
        assertTrue(t.containsSequence());
    }

    @Test
    void parseComplexTemplate() {
        // WL- {FIXED} - {DATE} - {SEQUENCE}
        // tokens: "WL-", FIXED, "-", DATE, "-", SEQUENCE = 6 tokens
        // (trailing empty literal is filtered out)
        FormatTemplate t = FormatTemplate.parse(
                "WL-{FIXED:value=PR}-{DATE:format=yyyyMMdd}-{SEQUENCE:length=6;bizTag=MD:WL}");
        assertEquals(6, t.size());
        assertEquals(FormatTemplate.TokenType.LITERAL, t.getTokens().get(0).getType());
        assertEquals("WL-", t.getTokens().get(0).getLiteral());
        assertEquals(FormatTemplate.TokenType.FIXED, t.getTokens().get(1).getType());
        assertEquals(FormatTemplate.TokenType.LITERAL, t.getTokens().get(2).getType());
        assertEquals("-", t.getTokens().get(2).getLiteral());
        assertEquals(FormatTemplate.TokenType.DATE, t.getTokens().get(3).getType());
        assertEquals(FormatTemplate.TokenType.LITERAL, t.getTokens().get(4).getType());
        assertEquals(FormatTemplate.TokenType.SEQUENCE, t.getTokens().get(5).getType());
    }

    @Test
    void parseComplexTemplateWithTrailingLiteral() {
        // {FIXED} -POST  => 2 tokens (FIXED, "-POST")
        FormatTemplate t = FormatTemplate.parse("{FIXED:value=A}-POST");
        assertEquals(2, t.size());
        assertEquals(FormatTemplate.TokenType.FIXED, t.getTokens().get(0).getType());
        assertEquals(FormatTemplate.TokenType.LITERAL, t.getTokens().get(1).getType());
        assertEquals("-POST", t.getTokens().get(1).getLiteral());
    }

    @Test
    void parseUnknownSegmentTypeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                FormatTemplate.parse("{UNKNOWN:value=x}"));
    }

    @Test
    void parseReferenceSeq() {
        FormatTemplate t = FormatTemplate.parse("{REFERENCE_SEQ:length=4;refField=contractCode}");
        assertEquals(FormatTemplate.TokenType.REFERENCE_SEQ, t.getTokens().get(0).getType());
        assertTrue(t.containsSequence());
    }

    @Test
    void renderRoundTrip() {
        String original = "{FIXED:value=PR}-{SEQUENCE:length=6;bizTag=MD:WL}";
        FormatTemplate t = FormatTemplate.parse(original);
        String rendered = t.render();
        FormatTemplate t2 = FormatTemplate.parse(rendered);
        assertEquals(t.size(), t2.size());
    }
}
