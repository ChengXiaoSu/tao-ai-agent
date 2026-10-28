package com.zt.taoaiagent.demo.invoke;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 *  测试HTTP方式接入大模型
 */
public class HttpAiInvoke {

    public static void main(String[] args) throws Exception {
        // 1. 从环境变量读取 API Key，避免硬编码
//        String apiKey = System.getenv("DASHSCOPE_API_KEY");
//        if (apiKey == null || apiKey.isBlank()) {
//            System.err.println("请先设置环境变量 DASHSCOPE_API_KEY");
//            return;
//        }

        // 2. 接口地址，workspace ID 按你的实际情况确认
        String url = "https://ws-3sj0n01mpib5bvt2.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

        // 3. 请求体 JSON，对应 curl 里的 -d
        String json = """
                {
                    "model": "qwen3.8-max",
                    "input": {
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "image": "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg"
                                    },
                                    {
                                        "text": "图中描绘的是什么景象?"
                                    }
                                ]
                            }
                        ]
                    }
                }
                """;

        // 4. 创建 HttpClient
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // 5. 构造 POST 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + TestApiKey.API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // 6. 发送请求并打印响应
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("HTTP 状态码: " + response.statusCode());
        System.out.println("响应体:");
        System.out.println(response.body());
    }
}
