package com.bidinote.storage.sqlite;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 简易 JSON 数组序列化，满足 aliases/tags 存储需求。
 */
public final class JsonUtils {
    private JsonUtils() {
    }

    public static String toJsonArray(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .map(value -> value.replace("\"", "\\\""))
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static Set<String> fromJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashSet<>();
        }
        String trimmed = json.trim();
        if (trimmed.length() <= 2) {
            return new LinkedHashSet<>();
        }
        String content = trimmed.substring(1, trimmed.length() - 1);
        if (content.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(content.split(","))
                .map(String::trim)
                .map(item -> item.replaceAll("^\"|\"$", ""))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
