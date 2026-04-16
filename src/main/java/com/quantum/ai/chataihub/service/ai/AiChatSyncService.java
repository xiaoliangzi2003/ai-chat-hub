package com.quantum.ai.chataihub.service.ai;

import com.quantum.ai.chataihub.constant.RedisKeys;
import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author xuhaodong
 * @date 2026/4/16 14:41
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AiChatAsyncSaveService aiChatAsyncSaveService;

    // 每小时同步一次
    @Scheduled(cron = "0 0 * * * ?")
    public void syncRedisChatToMongoDB() {
        System.out.println("===== 开始异步同步 Redis → MongoDB =====");
        String pattern = RedisKeys.DEEPSEEK_CACHE_KEY + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            Long userId = null;
            try {
                userId = Long.parseLong(key.replace(RedisKeys.DEEPSEEK_CACHE_KEY, ""));
                List<ChatMessage> messageList = (List<ChatMessage>) redisTemplate.opsForValue().get(key);
                if (messageList == null || messageList.isEmpty()) continue;

                // 异步同步，不阻塞定时任务
                aiChatAsyncSaveService.asyncSyncRedisToMongoDB(userId, messageList);
            } catch (Exception e) {
                log.error("同步 Redis → MongoDB 失败，用户 ID: {}, 错误信息: {}", userId, e.getMessage());
            }
        }
    }

}
