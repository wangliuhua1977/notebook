package com.bidinote.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理，读写 ~/.bidinote/config.json。
 */
public class AppConfig {
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".bidinote");
    private final Path configFile = configDir.resolve("config.json");
    private Map<String, String> values = new HashMap<>();

    public AppConfig() {
        values.put("dbPath", configDir.resolve("notes.db").toString());
        values.put("theme", "light");
        values.put("locale", "zh_CN");
    }

    public void load() {
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(configFile)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(configFile)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int len;
                while ((len = reader.read(buffer)) > 0) {
                    sb.append(buffer, 0, len);
                }
                parseJson(sb.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取配置失败", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                writer.write(toJson());
            }
        } catch (IOException e) {
            throw new IllegalStateException("保存配置失败", e);
        }
    }

    public String getDbPath() {
        return values.get("dbPath");
    }

    public String getTheme() {
        return values.getOrDefault("theme", "light");
    }

    public void setTheme(String theme) {
        values.put("theme", theme);
    }

    public String getLocale() {
        return values.getOrDefault("locale", "zh_CN");
    }

    private void parseJson(String json) {
        json = json.trim();
        json = json.substring(1, json.length() - 1);
        for (String entry : json.split(",")) {
            if (entry.isBlank()) continue;
            String[] pair = entry.split(":", 2);
            if (pair.length == 2) {
                String key = pair[0].trim().replaceAll("^\"|\"$", "");
                String value = pair[1].trim().replaceAll("^\"|\"$", "");
                values.put(key, value);
            }
        }
    }

    private String toJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append('\"').append(entry.getKey()).append('\"').append(':').append('\"').append(entry.getValue()).append('\"');
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }
}
