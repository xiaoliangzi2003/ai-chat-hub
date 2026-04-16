package com.quantum.ai.chataihub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置（AI聊天记录保存专用）
 *
 * @author xuhaodong
 * @date 2026/4/16 14:36
 */
@Configuration
public class AsyncConfig {
    @Bean("aiChatAsyncExecutor")
    public Executor aiChatAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数（IO密集型：核心数 = CPU核心数 * 2）
        executor.setCorePoolSize(8);
        // 最大线程数
        executor.setMaxPoolSize(16);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("ai-chat-async-");
        // 拒绝策略：调用者所在线程执行（不丢失消息）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
