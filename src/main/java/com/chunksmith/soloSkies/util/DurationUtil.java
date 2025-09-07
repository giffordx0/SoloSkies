package com.chunksmith.soloSkies.util;

import java.util.Locale;

public final class DurationUtil {
    private DurationUtil() {}

    /** Returns ticks for a duration like "30s", "5m", "2h", "1d". Plain digits = seconds. Null if invalid. */
    public static Long parseToTicks(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.trim().toLowerCase(Locale.ROOT);

        // plain digits => seconds
        if (s.matches("^\\d+$")) {
            long seconds = Long.parseLong(s);
            return secondsToTicks(seconds);
        }

        if (!s.matches("^\\d+[smhd]$")) return null;
        long n = Long.parseLong(s.substring(0, s.length() - 1));
        char unit = s.charAt(s.length() - 1);
        long seconds;
        switch (unit) {
            case 's': seconds = n; break;
            case 'm': seconds = n * 60L; break;
            case 'h': seconds = n * 3600L; break;
            case 'd': seconds = n * 86400L; break;
            default: return null;
        }
        return secondsToTicks(seconds);
    }

    public static boolean looksLikeDuration(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.matches("^\\d+$") || s.matches("^\\d+[smhd]$");
    }

    public static String pretty(String s) {
        // Return the normalized form for display; we keep user input as-is if valid
        return s;
    }

    private static long secondsToTicks(long seconds) {
        long ticks = seconds * 20L;
        if (ticks < 1) ticks = 1;
        return ticks;
    }
}