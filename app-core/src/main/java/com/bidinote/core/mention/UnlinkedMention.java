package com.bidinote.core.mention;

/**
 * 未链接提及候选，记录命中文本片段与目标节点 ID。
 */
public record UnlinkedMention(String noteId, String matchedText, int start, int end) {
}
