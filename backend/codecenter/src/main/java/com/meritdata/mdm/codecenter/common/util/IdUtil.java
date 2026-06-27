package com.meritdata.mdm.codecenter.common.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class IdUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private IdUtil() {}

    public static String simpleId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static String timestampId() {
        long ts = System.currentTimeMillis() / 1000;
        StringBuilder sb = new StringBuilder();
        sb.append(Long.toString(ts, 36));
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }

    public static String padSequence(long seq, int length) {
        String s = String.valueOf(seq);
        if (s.length() >= length) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - s.length(); i++) sb.append('0');
        sb.append(s);
        return sb.toString();
    }

    public static String padLeft(String s, int length, char pad) {
        if (s == null) s = "";
        if (s.length() >= length) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - s.length(); i++) sb.append(pad);
        sb.append(s);
        return sb.toString();
    }
}
