package com.quantum.ai.chataihub.repository;

import com.quantum.ai.chataihub.entity.ai.chat.AiChatMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author xuhaodong
 * @date 2026/4/16 14:48
 */
public interface AiChatMessageRepository extends MongoRepository<AiChatMessage, String> {
    // 根据会话ID查询所有消息
    List<AiChatMessage> findBySessionId(String sessionId);

    // 批量插入（定时任务用）
    @NotNull <S extends AiChatMessage> List<S> saveAll(@NotNull Iterable<S> entities);
}
