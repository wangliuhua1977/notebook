package com.bidinote.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 代表块到块或块到页面的链接边。
 */
public class NoteEdge {
    private final String srcBlockId;
    private final String srcPageId;
    private final String dstPageId;
    private final String dstBlockId;
    private final String type;
    private final String props;
    private final Instant createdAt;

    public NoteEdge(String srcBlockId, String srcPageId, String dstPageId, String dstBlockId, String type, String props, Instant createdAt) {
        this.srcBlockId = srcBlockId;
        this.srcPageId = Objects.requireNonNull(srcPageId, "srcPageId");
        this.dstPageId = Objects.requireNonNull(dstPageId, "dstPageId");
        this.dstBlockId = dstBlockId;
        this.type = type == null ? "link" : type;
        this.props = props;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getSrcBlockId() {
        return srcBlockId;
    }

    public String getSrcPageId() {
        return srcPageId;
    }

    public String getDstPageId() {
        return dstPageId;
    }

    public String getDstBlockId() {
        return dstBlockId;
    }

    public String getType() {
        return type;
    }

    public String getProps() {
        return props;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
