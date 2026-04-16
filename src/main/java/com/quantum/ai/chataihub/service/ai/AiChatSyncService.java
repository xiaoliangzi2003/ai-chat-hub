package com.quantum.ai.chataihub.service.ai;

import com.quantum.ai.chataihub.constant.RedisKeys;
import com.quantum.ai.chataihub.entity.ai.chat.AiChatSession;
import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import com.quantum.ai.chataihub.repository.AiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 定时同步 Redis 缓存到 MongoDB
 *
 * @author xuhaodong
 * @date 2026/4/16 14:41
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AiChatAsyncSaveService aiChatAsyncSaveService;
    private final AiChatSessionRepository sessionRepository;

    // 每小时同步一次
    @Scheduled(cron = "0 0 * * * ?")
    @SuppressWarnings("unchecked")
    public void syncRedisChatToMongoDB() {
        log.info("===== 开始异步同步 Redis → MongoDB =====");
        String pattern = RedisKeys.DEEPSEEK_CACHE_KEY + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            String sessionId = key.replace(RedisKeys.DEEPSEEK_CACHE_KEY, "");
            try {
                // 通过 sessionId 查找会话，获取 userId
                AiChatSession session = sessionRepository.findById(sessionId).orElse(null);
                if (session == null) {
                    log.warn("同步跳过：会话[{}]不存在", sessionId);
                    continue;
                }
                Long userId = session.getUserId();

                List<ChatMessage> messageList = (List<ChatMessage>) redisTemplate.opsForValue().get(key);
                if (messageList == null || messageList.isEmpty()) continue;

                // 异步同步，不阻塞定时任务
                aiChatAsyncSaveService.asyncSyncRedisToMongoDB(userId, sessionId, messageList);
            } catch (Exception e) {
                log.error("同步 Redis → MongoDB 失败，会话ID: {}, 错误信息: {}", sessionId, e.getMessage());
            }
        }
    }

}
