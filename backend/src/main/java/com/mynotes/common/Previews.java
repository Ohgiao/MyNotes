package com.mynotes.common;

import java.util.Locale;

public final class Previews {

    private Previews() {
    }

    public static String preview(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String flat = text.replace('\n', ' ').replace('\r', ' ').replaceAll(" {2,}", " ").strip();
        return flat.length() <= maxLen ? flat : flat.substring(0, maxLen) + "…";
    }

    /**
     * 命中位置居中截取，让搜索结果能看出"为什么命中"，而不是只回一个标题。
     */
    public static String snippet(String text, String query, int radius) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int idx = text.toLowerCase(Locale.ROOT).indexOf(query.toLowerCase(Locale.ROOT));
        if (idx < 0) {
            return preview(text, radius * 2);
        }
        int from = Math.max(0, idx - radius);
        int to = Math.min(text.length(), idx + query.length() + radius);
        String flat = text.substring(from, to).replace('\n', ' ').replaceAll(" {2,}", " ");
        return (from > 0 ? "…" : "") + flat + (to < text.length() ? "…" : "");
    }

    public static String firstLine(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                return preview(line, maxLen);
            }
        }
        return "";
    }
}
