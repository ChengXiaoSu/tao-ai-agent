package com.zt.taoaiagent.chatmemory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileBasedChatMemory 的纯本地单元测试（不启动 Spring、不调用大模型）：
 * 1) 写入后由新实例读回，验证真正的文件持久化；
 * 2) 超过窗口时只保留最近 maxMessages 条；
 * 3) 文件损坏时返回空列表并删除坏文件；
 * 4) clear 后读不到历史。
 */
class FileBasedChatMemoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistMessagesAcrossInstances() {
        String dir = tempDir.toString();
        new FileBasedChatMemory(dir, 10)
                .add("chat-1", List.of(new UserMessage("我叫程序员涛"), new AssistantMessage("你好，程序员涛")));

        List<Message> messages = new FileBasedChatMemory(dir, 10).get("chat-1");

        assertEquals(2, messages.size());
        assertEquals("我叫程序员涛", messages.get(0).getText());
        assertEquals("你好，程序员涛", messages.get(1).getText());
    }

    @Test
    void shouldKeepOnlyLatestMessagesWhenExceedingWindow() {
        FileBasedChatMemory memory = new FileBasedChatMemory(tempDir.toString(), 2);
        memory.add("chat-2", List.of(new UserMessage("第一条")));
        memory.add("chat-2", List.of(new UserMessage("第二条"), new AssistantMessage("第三条")));

        List<Message> messages = memory.get("chat-2");

        assertEquals(2, messages.size());
        assertEquals("第二条", messages.get(0).getText());
        assertEquals("第三条", messages.get(1).getText());
    }

    @Test
    void shouldReturnEmptyAndRemoveFileWhenCorrupted() throws IOException {
        FileBasedChatMemory memory = new FileBasedChatMemory(tempDir.toString(), 10);
        String fileName = Base64.getUrlEncoder().encodeToString("broken".getBytes(StandardCharsets.UTF_8)) + ".kryo";
        Path corrupted = tempDir.resolve(fileName);
        Files.writeString(corrupted, "not-a-kryo-file");

        assertTrue(memory.get("broken").isEmpty());
        assertFalse(Files.exists(corrupted), "损坏的记忆文件应被删除");
    }

    @Test
    void shouldClearConversation() {
        FileBasedChatMemory memory = new FileBasedChatMemory(tempDir.toString(), 10);
        memory.add("chat-3", List.of(new UserMessage("要被清除的消息")));

        memory.clear("chat-3");

        assertTrue(memory.get("chat-3").isEmpty());
    }
}
