package com.quantum.ai.chataihub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author xuhaodong
 * @date 2026/4/16 9:37
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.deepseek")
public class DeepSeekProperties {
    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 接口地址
     */
    private String baseUrl;

    /**
     * 对话配置
     */
    private Chat chat = new Chat();

    @Data
    public static class Chat {
        private Options options = new Options();
    }

    @Data
    public static class Options {
        /**
         * 模型名称
         */
        private String model;
    }
}
