package com.quantum.ai.chataihub.repository;

import com.quantum.ai.chataihub.entity.ai.chat.AiChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author xuhaodong
 * @date 2026/4/16 14:47
 */
public interface AiChatSessionRepository extends MongoRepository<AiChatSession, String> {
    // 根据用户ID查询会话
    List<AiChatSession> findByUserId(Long userId);

    // 根据用户ID+会话标题查询（判重）
    AiChatSession findByUserIdAndSessionTitle(Long userId, String title);
}
