package com.bidinote.core.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 笔记页面实体。以 ULID 作为主键，重命名时依赖此 ID 保持稳定链接。
 */
public class NoteNode {
    private final String id;
    private String title;
    private final Set<String> aliases = new LinkedHashSet<>();
    private final Set<String> tags = new LinkedHashSet<>();
    private Instant updatedAt;

    public NoteNode(String id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public Set<String> getAliases() {
        return Collections.unmodifiableSet(aliases);
    }

    public void addAlias(String alias) {
        if (alias != null && !alias.isBlank()) {
            aliases.add(alias.trim());
        }
    }

    public void addAliases(Set<String> newAliases) {
        if (newAliases != null) {
            newAliases.stream().filter(a -> a != null && !a.isBlank()).map(String::trim).forEach(aliases::add);
        }
    }

    public void clearAliases() {
        aliases.clear();
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public void setTags(Set<String> newTags) {
        tags.clear();
        if (newTags != null) {
            newTags.stream().filter(t -> t != null && !t.isBlank()).map(String::trim).forEach(tags::add);
        }
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteNode noteNode = (NoteNode) o;
        return id.equals(noteNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title;
    }
}
