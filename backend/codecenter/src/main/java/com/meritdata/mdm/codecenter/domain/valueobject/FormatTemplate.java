package com.meritdata.mdm.codecenter.domain.valueobject;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 格式模板值对象
 *
 * 模板语法: {TYPE:KEY=VALUE;KEY=VALUE}{TYPE:...}
 *   示例: WL-{FIXED:value=PR}-{DATE:format=yyyyMMdd}-{SEQUENCE:length=6;bizTag=MD:WL}
 *
 * V0.3 DSL 语法规范:
 *   1. 用 {} 标识码段
 *   2. 类型枚举: FIXED / DATE / SEQUENCE / EIGENVALUE / REFERENCE / REFERENCE_SEQ
 *   3. 段内配置: key=value;key=value
 *   4. 段外普通文本直接拼接
 */
@Data
@Builder
public class FormatTemplate {

    /** 原始模板字符串 */
    private final String rawTemplate;

    /** 解析后的所有 token（混合普通文本 + 码段） */
    @Builder.Default
    private final List<Token> tokens = new ArrayList<>();

    /** 模板是否包含流水码段 */
    public boolean containsSequence() {
        return tokens.stream().anyMatch(t -> t.type == TokenType.SEQUENCE
                || t.type == TokenType.REFERENCE_SEQ);
    }

    public boolean containsDate() {
        return tokens.stream().anyMatch(t -> t.type == TokenType.DATE);
    }

    public int size() {
        return tokens.size();
    }

    public enum TokenType {
        LITERAL,       // 普通文本
        FIXED,
        DATE,
        SEQUENCE,
        EIGENVALUE,
        REFERENCE,
        REFERENCE_SEQ
    }

    @Data
    @Builder
    public static class Token {
        private TokenType type;
        private String literal;
        private java.util.Map<String, String> params;
        private String originalText;
    }

    /** DSL token pattern: matches {TYPE:...} */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([A-Z_]+)(?::([^}]*))?\\}");

    /**
     * 解析原始模板
     */
    public static FormatTemplate parse(String raw) {
        if (raw == null) raw = "";

        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(raw);
        int lastEnd = 0;

        while (matcher.find()) {
            // 处理 token 之前的普通文本
            if (matcher.start() > lastEnd) {
                String literal = raw.substring(lastEnd, matcher.start());
                if (!literal.isEmpty()) {
                    tokens.add(Token.builder()
                            .type(TokenType.LITERAL)
                            .literal(literal)
                            .originalText(literal)
                            .build());
                }
            }

            String typeStr = matcher.group(1);
            String paramsStr = matcher.group(2);
            TokenType type;
            try {
                type = TokenType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown segment type: " + typeStr
                        + " in template: " + raw);
            }

            java.util.Map<String, String> params = new java.util.HashMap<>();
            if (paramsStr != null && !paramsStr.isEmpty()) {
                String[] pairs = paramsStr.split(";");
                for (String pair : pairs) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) {
                        params.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                    }
                }
            }

            tokens.add(Token.builder()
                    .type(type)
                    .params(params)
                    .originalText(matcher.group(0))
                    .build());

            lastEnd = matcher.end();
        }

        // 处理尾部普通文本
        if (lastEnd < raw.length()) {
            String literal = raw.substring(lastEnd);
            if (!literal.isEmpty()) {
                tokens.add(Token.builder()
                        .type(TokenType.LITERAL)
                        .literal(literal)
                        .originalText(literal)
                        .build());
            }
        }

        return FormatTemplate.builder()
                .rawTemplate(raw)
                .tokens(tokens)
                .build();
    }

    /**
     * 重新生成模板字符串（用于 UI 展示）
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            if (t.type == TokenType.LITERAL) {
                sb.append(t.literal);
            } else {
                sb.append("{").append(t.type.name());
                if (t.params != null && !t.params.isEmpty()) {
                    sb.append(":");
                    t.params.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));
                    if (sb.charAt(sb.length() - 1) == ';') sb.setLength(sb.length() - 1);
                }
                sb.append("}");
            }
        }
        return sb.toString();
    }
}
