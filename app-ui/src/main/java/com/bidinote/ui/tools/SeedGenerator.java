package com.bidinote.ui.tools;

import com.bidinote.core.NoteService;
import com.bidinote.core.model.NoteNode;
import com.bidinote.storage.sqlite.SQLiteNoteRepository;
import com.bidinote.ui.AppConfig;

import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 生成示例数据。
 */
public class SeedGenerator {
    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        config.load();
        SQLiteNoteRepository repository = new SQLiteNoteRepository(Paths.get(config.getDbPath()));
        NoteService service = new NoteService(repository);
        Random random = new Random(42);
        IntStream.range(0, 50).forEach(i -> {
            NoteNode node = new NoteNode("NODE" + i, "页面" + i);
            repository.saveNode(node);
            StringBuilder content = new StringBuilder();
            content.append("# 页面").append(i).append('\n');
            content.append("这是第").append(i).append("篇示例笔记，包含一些文字和引用。\n");
            if (i > 0) {
                int target = random.nextInt(i);
                content.append("引用 [[页面").append(target).append("]] 和 ![[页面").append(target).append("]]。\n");
            }
            content.append("段落结尾。\n");
            service.save(node, content.toString());
        });
        System.out.println("示例数据生成完成");
    }
}
