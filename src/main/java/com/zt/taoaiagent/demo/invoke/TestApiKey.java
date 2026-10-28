package com.zt.taoaiagent.demo.invoke;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author javatao
 * @date 2026/10/27 17:38
 */
@Component
public class TestApiKey {

    //阿里云百炼平台 apikey
    @Value("${tao-ai.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}