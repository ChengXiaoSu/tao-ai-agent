package com.zt.taoaiagent.demo.invoke;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *  测试LangChain接入大模型
 */
// 取消注释即可在 SpringBoot 项目启动时执行
//@Component
public class LangChainAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel chatModel; // Starter 自动注入的 DeepSeek ChatModel

    @Override
    public void run(String... args) {
        String response = chatModel.chat("你好，我是程序员涛");
        System.out.println(response);
    }
}

