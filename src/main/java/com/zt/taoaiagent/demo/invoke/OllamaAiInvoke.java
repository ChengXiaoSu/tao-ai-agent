package com.zt.taoaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *  ollama 接入本地部署大模型
 */
// 取消注释即可在 SpringBoot 项目启动时执行
//@Component
public class OllamaAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage output = ollamaChatModel.call(new Prompt("你好，我是程序员涛，写一篇800字作文，与程序员相关的"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }
}

