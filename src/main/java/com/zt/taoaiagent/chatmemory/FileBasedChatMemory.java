package com.zt.taoaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于文件持久化的对话记忆（Kryo 序列化 + 原子替换写）。
 * <p>
 * 关键设计：
 * 1) 所有读写操作加锁串行化，避免并发丢消息，同时保证共享 static Kryo 的线程安全；
 * 2) 先写临时文件再原子替换，进程中断不会留下损坏文件；
 * 3) 读取失败（文件损坏）时删除文件并返回空列表，不让异常打穿对话链路；
 * 4) chatId 经 Base64 编码后作为文件名，防止路径穿越。
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private static final int DEFAULT_MAX_MESSAGES = 20;

    private final String baseDir;
    private final int maxMessages;

    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        // 设置实例化策略：绕过构造器创建对象，保证反序列化 Spring AI 消息成功
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    // 全局锁串行化读写：add 是"读-改-写"三步，必须原子化，否则并发丢消息
    private final ReentrantLock lock = new ReentrantLock();

    // 构造对象时，指定文件保存目录（默认最多保留 20 条消息）
    public FileBasedChatMemory(String dir) {
        this(dir, DEFAULT_MAX_MESSAGES);
    }

    public FileBasedChatMemory(String dir, int maxMessages) {
        this.baseDir = dir;
        this.maxMessages = maxMessages;
        File base = new File(dir);
        if (!base.exists()) {
            base.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        lock.lock();
        try {
            List<Message> conversationMessages = getOrCreateConversation(conversationId);
            conversationMessages.addAll(messages);
            // 超出窗口时丢弃最早的消息，避免文件无限增长
            if (conversationMessages.size() > maxMessages) {
                conversationMessages = new ArrayList<>(conversationMessages.subList(
                        conversationMessages.size() - maxMessages, conversationMessages.size()));
            }
            saveConversation(conversationId, conversationMessages);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        lock.lock();
        try {
            return getOrCreateConversation(conversationId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear(String conversationId) {
        lock.lock();
        try {
            File file = getConversationFile(conversationId);
            if (file.exists() && !file.delete()) {
                log.warn("删除对话记忆文件失败: {}", file.getName());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 读取会话历史；文件不存在或损坏时返回空列表，绝不让异常打穿调用方。
     */
    @SuppressWarnings("unchecked")
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                return (List<Message>) kryo.readObject(input, ArrayList.class);
            } catch (KryoException e) {
                // KryoException 是 RuntimeException，必须单独捕获；文件损坏时删除并重置
                log.error("对话记忆文件损坏，已删除并重置会话: {}", file.getName(), e);
                file.delete();
            } catch (IOException e) {
                log.error("读取对话记忆文件失败: {}", file.getName(), e);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 先写 .tmp 临时文件，成功后再原子替换为目标文件，避免进程中断留下半截损坏文件。
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            // 先写临时文件并关闭流：Kryo 的 close() 会先 flush 缓冲再关闭底层文件流，
            // 保证数据完整落盘且释放 Windows 文件句柄
            try (Output output = new Output(new FileOutputStream(tmp))) {
                kryo.writeObject(output, messages);
            }
            // 流完全关闭后再替换目标文件：Windows 无法移动（重命名）仍被打开的文件，
            // 否则会抛 ERROR_SHARING_VIOLATION（"另一个程序正在使用此文件"）
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存对话记忆文件失败: {}", file.getName(), e);
            tmp.delete();
        }
    }

    private File getConversationFile(String conversationId) {
        // chatId 可能含 ../、\、: 等字符，Base64 编码后作为文件名，防止路径穿越
        String safeName = Base64.getUrlEncoder().encodeToString(conversationId.getBytes(StandardCharsets.UTF_8));
        return new File(baseDir, safeName + ".kryo");
    }
}


