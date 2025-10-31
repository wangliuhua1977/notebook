package com.bidinote.core.suggest;

import com.bidinote.core.model.NoteNode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于简单 TF-IDF 的链接建议。这里使用标题、别名与标签计算分数。
 */
public class LinkSuggestionService {
    private final Map<String, NoteNode> notesById;

    public LinkSuggestionService(Map<String, NoteNode> notesById) {
        this.notesById = Objects.requireNonNull(notesById, "notesById");
    }

    public List<NoteNode> suggest(String paragraphText, int limit) {
        if (paragraphText == null || paragraphText.isBlank()) {
            return List.of();
        }
        Map<String, Long> tf = Arrays.stream(paragraphText.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.groupingBy(token -> token, Collectors.counting()));
        return notesById.values().stream()
                .filter(node -> node.getTitle() != null)
                .map(node -> Map.entry(node, score(node, tf)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<NoteNode, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double score(NoteNode node, Map<String, Long> tf) {
        Set<String> tokens = buildTokens(node);
        double score = 0.0;
        for (String token : tokens) {
            score += tf.getOrDefault(token, 0L);
        }
        return score;
    }

    private Set<String> buildTokens(NoteNode node) {
        return Arrays.stream((node.getTitle() + " " + String.join(" ", node.getAliases()) + " " + String.join(" ", node.getTags()))
                        .toLowerCase(Locale.ROOT)
                        .split("\\W+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toSet());
    }
}
