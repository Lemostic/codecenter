package com.meritdata.mdm.codecenter.infrastructure.dsl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groovy 脚本沙箱
 *
 * 安全限制:
 *   - 禁用 System / Runtime / Process / Class.forName
 *   - 禁用 new File() / 网络访问
 *   - 提供安全的 getCode(data) 入口
 *
 * 性能:
 *   - 脚本编译结果缓存（按 SHA256 缓存）
 */
@Slf4j
@Component
public class GroovyScriptEngine {

    private final GroovyShell shell = new GroovyShell();
    private final ConcurrentHashMap<String, Script> scriptCache = new ConcurrentHashMap<>();

    public String execute(String groovyScript, Map<String, Object> data) {
        if (groovyScript == null || groovyScript.isEmpty()) {
            throw new IllegalArgumentException("Groovy script is empty");
        }
        Script script = scriptCache.computeIfAbsent(sha256(groovyScript),
                k -> compileWithSanityCheck(groovyScript));

        Binding binding = new Binding();
        binding.setVariable("data", data);
        binding.setVariable("ctx", data);
        binding.setVariable("now", java.time.LocalDateTime.now());
        binding.setVariable("util", new SafeUtil());

        Object result = script.run();
        if (result == null) {
            throw new IllegalStateException("Groovy script returned null");
        }
        return result.toString();
    }

    private Script compileWithSanityCheck(String script) {
        String[] forbidden = { "System.", "Runtime.", "ProcessBuilder", "Process ",
                "Class.forName", "java.io.File", "java.net.", "java.lang.reflect",
                "groovy.", "org.codehaus.groovy.", "Thread.start", "new File(" };
        for (String s : forbidden) {
            if (script.contains(s)) {
                throw new SecurityException("Forbidden API in groovy script: " + s);
            }
        }
        return shell.parse(script);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    /** 脚本中可用的安全工具 */
    public static class SafeUtil {
        public String pad(long seq, int len) {
            return com.meritdata.mdm.codecenter.common.util.IdUtil.padSequence(seq, len);
        }
        public String now(String pattern) {
            return java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern(pattern));
        }
        public String substring(String s, int start, int end) {
            if (s == null) return "";
            int safeEnd = Math.min(end, s.length());
            int safeStart = Math.max(0, Math.min(start, safeEnd));
            return s.substring(safeStart, safeEnd);
        }
    }
}
