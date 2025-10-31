package com.bidinote.core.model;

import java.util.Objects;

/**
 * 表示页面中的一个块，可对应段落、标题或列表项。
 */
public class NoteBlock {
    private final String id;
    private final String pageId;
    private final String text;
    private final String anchor;
    private final int orderNo;

    public NoteBlock(String id, String pageId, String text, String anchor, int orderNo) {
        this.id = Objects.requireNonNull(id, "id");
        this.pageId = Objects.requireNonNull(pageId, "pageId");
        this.text = text == null ? "" : text;
        this.anchor = anchor;
        this.orderNo = orderNo;
    }

    public String getId() {
        return id;
    }

    public String getPageId() {
        return pageId;
    }

    public String getText() {
        return text;
    }

    public String getAnchor() {
        return anchor;
    }

    public int getOrderNo() {
        return orderNo;
    }
}
