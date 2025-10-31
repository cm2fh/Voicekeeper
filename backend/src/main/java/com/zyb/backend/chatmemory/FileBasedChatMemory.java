package com.zyb.backend.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件的ChatMemory实现
 * 使用Kryo序列化，作为Redis的备份存储
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;

    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        // 注册Spring AI相关类型
        kryo.register(UserMessage.class);
        kryo.register(AssistantMessage.class);
        kryo.register(SystemMessage.class);
        kryo.register(ToolResponseMessage.class);
        kryo.register(Media.class);
        kryo.register(AssistantMessage.ToolCall.class);
        kryo.register(ToolResponseMessage.ToolResponse.class);
        kryo.register(MessageType.class);
    }

    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (created) {
                log.info("创建对话记忆目录: {}", dir);
            }
        }
    }

    @Override
    public void add(@NotNull String conversationId, @NotNull Message message) {
        // 获取现有对话历史
        List<Message> messageList = getOrCreateConversation(conversationId);
        // 追加新消息
        messageList.add(message);
        // 保存完整的对话历史
        saveConversation(conversationId, messageList);
    }

    @Override
    public void add(@NotNull String conversationId, @NotNull List<Message> messages) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        saveConversation(conversationId, messageList);
    }

    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    @Override
    public void clear(@NotNull String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.debug("删除对话文件: {}", conversationId);
            }
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                List<Message> loadedMessages = kryo.readObject(input, ArrayList.class);
                if (loadedMessages != null) {
                    // 过滤掉 null 消息
                    for (Message msg : loadedMessages) {
                        if (msg != null) {
                            messages.add(msg);
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("读取对话文件失败: {}", e.getMessage());
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("已删除损坏的对话文件");
                }
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
            output.flush();
            log.debug("保存对话文件: {} ({} 条消息)", conversationId, messages.size());
        } catch (IOException e) {
            log.error("保存对话文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存对话文件失败", e);
        }
    }

    private File getConversationFile(String conversationId) {
        // 替换文件名中的非法字符
        String safeFileName = conversationId
                .replace(":", "_")
                .replace("/", "_")
                .replace("\\", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_")
                .replace(" ", "_")
                .replaceAll("[\\x00-\\x1f]", "_");

        if (safeFileName.isEmpty()) {
            safeFileName = "default_conversation";
        }
        if (safeFileName.length() > 100) {
            safeFileName = safeFileName.substring(0, 100);
        }

        return new File(BASE_DIR, safeFileName + ".kryo");
    }
}

