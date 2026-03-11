package com.matchimban.matchimban_api.global.storage;

import org.springframework.stereotype.Component;

@Component
public class CdnUrlComposer {

    private final String baseUrl;

    public CdnUrlComposer(AppCdnProperties props) {
        this.baseUrl = trimTrailingSlash(props.getBaseUrl());
    }

    public String toPublicUrl(String keyOrUrl) {
        if (keyOrUrl == null) return null;
        String v = keyOrUrl.trim();
        if (v.isEmpty()) return null;

        if (startsWithHttpScheme(v)) return v;

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.cdn.base-url is not configured, but objectKey is provided: " + v);
        }

        String key = trimLeadingSlash(v);
        return baseUrl + "/" + key;
    }

    private static boolean startsWithHttpScheme(String value) {
        return value.regionMatches(true, 0, "http://", 0, "http://".length())
                || value.regionMatches(true, 0, "https://", 0, "https://".length());
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        int end = t.length();
        while (end > 0 && t.charAt(end - 1) == '/') end--;
        return t.substring(0, end);
    }

    private static String trimLeadingSlash(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        int start = 0;
        while (start < t.length() && t.charAt(start) == '/') start++;
        return t.substring(start);
    }
}