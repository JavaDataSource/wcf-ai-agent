package com.wcf.wcfaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

/**
 * LangChain4j 框架调用 AI 大模型
 */
public class LangChainAiInvoke {

    public static void main(String[] args) {
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        String answer = qwenChatModel.chat("我是程序员wcf，这是编程导航 codefather.cn 的AI超级智能体原创项目");
        System.out.println(answer);
    }

}
