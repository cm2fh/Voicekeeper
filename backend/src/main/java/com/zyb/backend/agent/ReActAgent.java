package com.zyb.backend.agent;

import com.zyb.backend.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct模式的智能体抽象类
 * AI原生应用的核心决策模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    private String finalAnswer;

    /**
     * 思考：处理当前状态并决定下一步行动
     * @return 是否需要执行行动
     */
    public abstract boolean think();

    /**
     * 行动：执行决定的行动
     * @return 执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考 → 行动
     */
    @Override
    public String step() {
        // 重置最终答案
        setFinalAnswer(null);

        log.info("思考中...");
        boolean shouldAct = think();

        if (shouldAct) {
            log.info("行动中...");
            return act();
        }

        // 如果有最终答案，则返回它并结束
        String answer = getFinalAnswer();
        if (answer != null && !answer.isEmpty()) {
            setState(AgentState.FINISHED);
            log.info("✅ 思考完成：提供最终答案");
            return answer;
        }

        log.info("✅ 思考完成：无需行动");
        setState(AgentState.FINISHED);
        return "思考完成 - 无需行动";
    }

    /**
     * 清理资源
     */
    @Override
    protected void cleanup() {
        super.cleanup();
        this.finalAnswer = null;
    }
}

