package com.quantum.ai.chataihub.service.ai;

import com.quantum.ai.chataihub.entity.ai.chat.AiChatMessage;
import com.quantum.ai.chataihub.entity.ai.chat.AiChatSession;
import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import com.quantum.ai.chataihub.repository.AiChatMessageRepository;
import com.quantum.ai.chataihub.repository.AiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 聊天记录 异步持久化服务
 *
 * @author xuhaodong
 * @date 2026/4/16 14:37
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatAsyncSaveService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;

    /**
     * 实时保存聊天记录
     */
    @Async("aiChatAsyncExecutor")
    public void asyncSaveChatRecord(Long userId, List<ChatMessage> messageList) {
        try {
            // 创建/更新会话
            AiChatSession session = getOrCreateSession(userId);
            // 批量保存消息
            batchSaveMessages(session.getId(), userId, messageList);
            log.info("✅ 异步保存用户[{}]聊天记录成功", userId);
        } catch (Exception e) {
            log.error("❌ 异步保存聊天记录失败：{}", e.getMessage());
        }
    }

    /**
     * 【异步】定时任务同步Redis数据（异步执行，不阻塞调度线程）
     */
    @Async("aiChatAsyncExecutor")
    public void asyncSyncRedisToMongoDB(Long userId, List<ChatMessage> messageList) {
        try {
            AiChatSession session = getOrCreateSession(userId);
            batchSaveMessages(session.getId(), userId, messageList);
        } catch (Exception e) {
            log.error("同步用户[{}]会话失败：{}", userId, e.getMessage());
        }
    }

    /**
     * 【异步】清空用户会话（补全的方法）
     */
    @Async("aiChatAsyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void asyncDeleteSession(Long userId) {
        try {
            List<AiChatSession> sessionList = sessionRepository.findByUserId(userId);
            if (sessionList.isEmpty()) {
                System.out.println("用户[" + userId + "]无会话可删除");
                return;
            }

            for (AiChatSession session : sessionList) {
                // 软删会话
                session.setIsDelete(true);
                sessionRepository.save(session);
                // 删除关联消息
                List<AiChatMessage> messageList = messageRepository.findBySessionId(session.getId());
                if (!messageList.isEmpty()) {
                    messageRepository.deleteAll(messageList);
                }
            }
            System.out.println("✅ 异步删除用户[" + userId + "]会话成功");
        } catch (Exception e) {
            System.err.println("❌ 异步删除用户[" + userId + "]会话失败：" + e.getMessage());
        }
    }

    private AiChatSession getOrCreateSession(Long userId) {
        AiChatSession session = sessionRepository.findByUserIdAndSessionTitle(userId, "默认对话");
        if (session == null) {
            session = new AiChatSession();
            session.setUserId(userId);
            session.setSessionTitle("默认对话");
            session.setCreateTime(new Date());
        }
        session.setUpdateTime(new Date());
        return sessionRepository.save(session);
    }

    private void batchSaveMessages(String sessionId, Long userId, List<ChatMessage> messageList) {
        List<AiChatMessage> mongoMessages = messageList.stream().map(msg -> {
            AiChatMessage mongoMsg = new AiChatMessage();
            mongoMsg.setSessionId(sessionId);
            mongoMsg.setUserId(userId);
            mongoMsg.setRole(msg.getRole());
            mongoMsg.setContent(msg.getContent());
            mongoMsg.setToolCalls(msg.getToolCalls());
            mongoMsg.setToolCallId(msg.getToolCallId());
            mongoMsg.setCreateTime(new Date());
            mongoMsg.setTokens(0);
            return mongoMsg;
        }).collect(Collectors.toList());

        messageRepository.saveAll(mongoMessages);
    }
}
