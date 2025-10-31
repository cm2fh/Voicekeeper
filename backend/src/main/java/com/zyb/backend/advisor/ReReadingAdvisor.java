package com.zyb.backend.advisor;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    private ChatClientRequest before(ChatClientRequest chatClientRequest) {
        String userText = chatClientRequest.prompt().getUserMessage().getText();
        // 添加上下文参数
        chatClientRequest.context().put("re2_input_query", userText);
        // 修改用户提示词
        String newUserText = """
                %s
                Read the question again: %s
                """.formatted(userText, userText);
        Prompt newPrompt = chatClientRequest.prompt().augmentUserMessage(newUserText);
        return new ChatClientRequest(newPrompt, chatClientRequest.context());
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest advisedRequest, CallAdvisorChain chain) {
        return chain.nextCall(this.before(advisedRequest));
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest advisedRequest, StreamAdvisorChain chain) {
        return chain.nextStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
