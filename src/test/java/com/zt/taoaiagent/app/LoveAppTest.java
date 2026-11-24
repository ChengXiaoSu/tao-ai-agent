package com.zt.taoaiagent.app;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;



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
        message = "我想让我的另一半（上官浅）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);

        //第四轮对话
        message = "你是情感方面的专家吗";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);

        //第五轮对话
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(message, answer);
    }

    @Test
    void doChatWithReport() {

        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员橙小酥, 我想让我的另一半上官浅更爱我 ，但我不知道怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void testFileBasedChat1() {
        // 注意：chatId 必须固定，不能用 UUID，否则重启后对不上
        String chatId = "persist-test-001";

        //第一轮对话：写入记忆后进程结束
        String message = "我叫程序员涛，请记住这个名字";
        String answer = loveApp.doChat(message, chatId);
        System.out.println("第一轮回答: " + answer);
    }

    @Test
    void testFileBasedChat2() {
        // 与第一次运行使用同一个 chatId
        String chatId = "persist-test-001";

        // 只问上一轮说过的事，看模型能否从文件记忆里答出来
        String message = "我刚才说我叫什么名字？";
        String answer = loveApp.doChat(message, chatId);
        System.out.println("第二轮回答: " + answer);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么解决，直接给我建议不要问我额外的信息？";
        String answer =  loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithCloudRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么解决，直接给我建议不要问我额外的信息？";
        String answer =  loveApp.doChatWithCloudRag(message, chatId);
        Assertions.assertNotNull(answer);
    }



}