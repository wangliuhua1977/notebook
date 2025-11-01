package com.bidinote.core.parser;

import java.util.Objects;

/**
 * `[[页面#标题]]` 或 `[[页面^块ID]]` 链接的解析结果。
 */
public class WikiLink {
    public enum Type {
        PAGE,
        HEADING,
        BLOCK,
        EMBED
    }

    private final String rawText;
    private final String targetTitle;
    private final String anchor;
    private final Type type;
    private final boolean embed;

    public WikiLink(String rawText, String targetTitle, String anchor, Type type, boolean embed) {
        this.rawText = Objects.requireNonNull(rawText);
        this.targetTitle = targetTitle;
        this.anchor = anchor;
        this.type = type;
        this.embed = embed;
    }

    public String getRawText() {
        return rawText;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public String getAnchor() {
        return anchor;
    }

    public Type getType() {
        return type;
    }

    public boolean isEmbed() {
        return embed;
    }
}
