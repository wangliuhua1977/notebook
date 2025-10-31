package com.bidinote.core.mention;

import com.bidinote.core.parser.WikiLink;
import com.bidinote.core.parser.WikiLinkParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 未链接提及识别：扫描纯文本命中词典，排除已链接部分。
 */
public class MentionDetector {
    private final AliasDictionary dictionary;
    private final WikiLinkParser linkParser = new WikiLinkParser();

    public MentionDetector(AliasDictionary dictionary) {
        this.dictionary = Objects.requireNonNull(dictionary, "dictionary");
    }

    public List<UnlinkedMention> detect(String text) {
        List<UnlinkedMention> mentions = new ArrayList<>();
        Set<Integer> linkedRange = new HashSet<>();
        for (WikiLink link : linkParser.parse(text)) {
            int start = text.indexOf(link.getRawText());
            if (start >= 0) {
                for (int i = start; i < start + link.getRawText().length(); i++) {
                    linkedRange.add(i);
                }
            }
        }
        for (int i = 0; i < text.length(); i++) {
            if (linkedRange.contains(i)) {
                continue;
            }
            for (int len = 2; len <= 20 && i + len <= text.length(); len++) {
                if (linkedRange.contains(i + len - 1)) {
                    break;
                }
                String segment = text.substring(i, i + len);
                for (String noteId : dictionary.lookup(segment)) {
                    mentions.add(new UnlinkedMention(noteId, segment, i, i + len));
                }
            }
        }
        return mentions;
    }
}
