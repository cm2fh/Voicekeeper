package com.zyb.backend.advisor;

import cn.hutool.core.io.resource.ClassPathResource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ForbiddenWordsAdvisor implements CallAdvisor, StreamAdvisor {

    private List<String> forbiddenWords;
    private final String filePath;

    public ForbiddenWordsAdvisor() {
        this("forbidden-words.txt");
    }

    public ForbiddenWordsAdvisor(String filePath) {
        this.filePath = filePath;
        loadForbiddenWords();
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        checkUserMessage(chatClientRequest);
        return chain.nextCall(chatClientRequest);
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest advisedRequest, StreamAdvisorChain chain) {
        checkUserMessage(advisedRequest);
        return chain.nextStream(advisedRequest);
    }

    /**
     * 检查用户最新的一条消息是否包含违禁词
     * @param request ChatClientRequest
     */
    private void checkUserMessage(ChatClientRequest request) {
        request.prompt().getInstructions().stream()
                .filter(UserMessage.class::isInstance)
                .reduce((first, second) -> second) // 获取最后一条用户消息
                .ifPresent(lastUserMessage -> checkForbiddenWords(lastUserMessage.getText()));
    }

    /**
     * 检查是否包含违禁词
     * @param content
     */
    private void checkForbiddenWords(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        List<String> foundWords = forbiddenWords.stream()
                .filter(word -> content.toLowerCase().contains(word.toLowerCase()))
                .collect(Collectors.toList());

        if (!foundWords.isEmpty()) {
            throw new RuntimeException("内容包含违禁词: " + String.join(", ", foundWords));
        }
    }

    /**
     * 加载文件
     */
    private void loadForbiddenWords() {
        try {
            // 尝试从类路径资源加载
            InputStream inputStream = new ClassPathResource(filePath).getStream();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                forbiddenWords = reader.lines()
                        .filter(line -> !line.trim().isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            // 如果类路径找不到，尝试从绝对路径加载
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    forbiddenWords = Files.readAllLines(path, StandardCharsets.UTF_8)
                            .stream()
                            .filter(line -> !line.trim().isEmpty())
                            .collect(Collectors.toList());
                } else {
                    forbiddenWords = new ArrayList<>();
                    log.error("无法加载违禁词文件: {}", filePath);
                }
            } catch (IOException ex) {
                forbiddenWords = new ArrayList<>();
                log.error("无法加载违禁词: {}", ex.getMessage());
            }
        }
    }

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}