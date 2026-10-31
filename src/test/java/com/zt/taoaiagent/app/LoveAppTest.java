package com.zt.taoaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();

        //第一轮对话
        String message = "你好，我是程序员涛";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);

        //第二轮对话
        message = "我有个情感上面的问题想咨询下你";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);

        //第三轮对话
        message = "你是情感方面的专家吗";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);

        //第四轮对话
        message = "我想让我的另一半（上官浅）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);

        //第五轮对话
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);
    }
}