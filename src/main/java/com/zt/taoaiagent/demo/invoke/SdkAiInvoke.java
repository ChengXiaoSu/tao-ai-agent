package com.zt.taoaiagent.demo.invoke;

import java.util.Arrays;
import java.util.Collections;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import com.zt.taoaiagent.TaoAiAgentApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * 测试SDK方式接入大模型
 */
public class SdkAiInvoke {

    static {
        Constants.baseHttpApiUrl = "https://ws-3sj0n01mpib5bvt2.cn-beijing.maas.aliyuncs.com/api/v1";
    }

    public static void simpleMultiModalConversationCall(TestApiKey testApiKey)
            throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("image", "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg"),
                        Collections.singletonMap("text", "图中描绘的是什么景象?还有你是谁"))).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(testApiKey.getApiKey())
                .model("qwen3.8-max")
                .messages(Arrays.asList(userMessage))
                .build();
        MultiModalConversationResult result = conv.call(param);
        System.out.println(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(TaoAiAgentApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(args)) {
            TestApiKey testApiKey = ctx.getBean(TestApiKey.class);
            simpleMultiModalConversationCall(testApiKey);
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            //使用日志记录异常信息
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}