package com.zt.taoaiagent.demo.invoke;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author javatao
 * @date 2026/10/27 17:38
 */
public interface TestApiKey {

    //阿里云百炼平台 apikey
//    @Value("${your-app.api.key}")
    String API_KEY = "Your Api Key";
}
