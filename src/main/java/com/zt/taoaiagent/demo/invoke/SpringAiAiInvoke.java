package com.zt.taoaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *  测试Spring AI接入大模型
 */
// 取消注释即可在 SpringBoot 项目启动时执行
//@Component
public class SpringAiAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel deepSeekChatModel; // Spring 会自动注入 DeepSeek 的 ChatModel

    @Override
    public void run(String... args) throws Exception {
        String response = deepSeekChatModel.call(new Prompt("你好，我是程序员涛"))
                .getResult()
                .getOutput()
                .getText();
        System.out.println(response);
    }
}

