package com.bidinote.core.mention;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 标题/别名词典，支持大小写、简繁、全角半角归一化。
 */
public class AliasDictionary {
    private final Map<String, Set<String>> normalizedToIds = new HashMap<>();

    public void addEntry(String noteId, String title, Set<String> aliases) {
        if (title != null) {
            index(noteId, title);
        }
        if (aliases != null) {
            aliases.forEach(alias -> index(noteId, alias));
        }
    }

    private void index(String noteId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = normalize(text);
        normalizedToIds.computeIfAbsent(normalized, k -> new HashSet<>()).add(noteId);
    }

    public Set<String> lookup(String rawText) {
        String normalized = normalize(rawText);
        return normalizedToIds.getOrDefault(normalized, Set.of());
    }

    private String normalize(String text) {
        String result = Normalizer.normalize(text, Normalizer.Form.NFKC);
        result = result.toLowerCase(Locale.ROOT);
        // 简繁转换可使用第三方库，此处使用占位逻辑。
        result = result.replace('臺', '台');
        result = result.replace('與', '与');
        return result;
    }
}
