package com.bidinote.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 Markdown 文本中的 wiki 链接、块引用与嵌入语法。
 */
public class WikiLinkParser {
    private static final Pattern LINK_PATTERN = Pattern.compile("!?\\[\\[([^\\]]+)\\]\\]");

    public List<WikiLink> parse(String text) {
        Objects.requireNonNull(text, "text");
        List<WikiLink> result = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group();
            String inside = matcher.group(1).trim();
            boolean embed = raw.startsWith("![");
            WikiLink.Type type = WikiLink.Type.PAGE;
            String anchor = null;
            String target = inside;
            if (inside.contains("#")) {
                String[] parts = inside.split("#", 2);
                target = parts[0].trim();
                anchor = parts[1].trim();
                type = WikiLink.Type.HEADING;
            }
            if (inside.contains("^")) {
                String[] parts = inside.split("\\^", 2);
                target = parts[0].trim();
                anchor = parts[1].trim();
                type = WikiLink.Type.BLOCK;
            }
            result.add(new WikiLink(raw, target, anchor, embed ? WikiLink.Type.EMBED : type, embed));
        }
        return result;
    }
}
