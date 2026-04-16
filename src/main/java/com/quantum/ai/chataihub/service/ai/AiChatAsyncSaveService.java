package com.quantum.ai.chataihub.service.ai;

import com.quantum.ai.chataihub.entity.ai.chat.AiChatMessage;
import com.quantum.ai.chataihub.entity.ai.chat.AiChatSession;
import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import com.quantum.ai.chataihub.repository.AiChatMessageRepository;
import com.quantum.ai.chataihub.repository.AiChatSessionRepository;
import com.quantum.ai.chataihub.vo.ai.SessionDetailVO;
import com.quantum.ai.chataihub.vo.ai.SessionListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
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
    public void asyncSaveChatRecord(Long userId, String sessionId, List<ChatMessage> messageList) {
        try {
            AiChatSession session = getSessionById(sessionId);
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            // 批量保存消息
            batchSaveMessages(sessionId, userId, messageList);
            log.info("✅ 保存会话[{}]聊天记录成功", sessionId);
        } catch (Exception e) {
            log.error("❌ 保存聊天记录失败：{}", e.getMessage());
        }
    }

    // 创建默认会话
    public String createDefaultSession(Long userId) {
        AiChatSession session = sessionRepository.findTopByUserIdOrderByUpdateTimeDesc(userId);
        if (session != null) {
            return session.getId();
        }
        // 新建默认会话：不手动设置ID，MongoDB自动生成ObjectId
        session = new AiChatSession();
        session.setUserId(userId);
        session.setSessionTitle("默认对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDelete(false);
        sessionRepository.save(session);
        return session.getId();
    }

    // ==================== 新增：获取会话列表 ====================
    public List<SessionListVO> getSessionListByUserId(Long userId) {
        List<AiChatSession> sessions = sessionRepository.findByUserIdAndIsDeleteFalse(userId);
        return sessions.stream().map(session -> {
            SessionListVO vo = new SessionListVO();
            BeanUtils.copyProperties(session, vo);
            vo.setSessionId(session.getId());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 新增：获取会话详情 ====================
    public SessionDetailVO getSessionDetail(String sessionId, Long userId) {
        AiChatSession session = sessionRepository.findByIdAndUserIdAndIsDeleteFalse(sessionId, userId);
        if (session == null) {
            throw new RuntimeException("会话不存在或无权限");
        }
        // 查询历史消息
        List<AiChatMessage> messageList = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        List<ChatMessage> history = messageList.stream().map(msg -> {
            ChatMessage chatMsg = new ChatMessage();
            chatMsg.setRole(msg.getRole());
            chatMsg.setContent(msg.getContent());
            chatMsg.setToolCalls(msg.getToolCalls());
            chatMsg.setToolCallId(msg.getToolCallId());
            return chatMsg;
        }).collect(Collectors.toList());

        SessionDetailVO vo = new SessionDetailVO();
        vo.setSessionId(sessionId);
        vo.setSessionTitle(session.getSessionTitle());
        vo.setHistoryMessages(history);
        return vo;
    }

    // ==================== 新增：根据sessionId获取历史消息 ====================
    public List<ChatMessage> getSessionMessages(String sessionId) {
        List<AiChatMessage> messageList = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        return messageList.stream().map(msg -> {
            ChatMessage chatMsg = new ChatMessage();
            chatMsg.setRole(msg.getRole());
            chatMsg.setContent(msg.getContent());
            return chatMsg;
        }).collect(Collectors.toList());
    }

    /**
     * 【异步】定时任务同步Redis数据（异步执行，不阻塞调度线程）
     */
    @Async("aiChatAsyncExecutor")
    public void asyncSyncRedisToMongoDB(Long userId, String sessionId, List<ChatMessage> messageList) {
        try {
            batchSaveMessages(sessionId, userId, messageList);
            log.info("✅ 同步会话[{}]到MongoDB成功", sessionId);
        } catch (Exception e) {
            log.error("同步会话[{}]失败：{}", sessionId, e.getMessage());
        }
    }

    /**
     * 【异步】根据会话Id清空用户会话
     */
    @Async("aiChatAsyncExecutor")
    public void asyncDeleteSessionBySessionId(String sessionId) {
        try {
            AiChatSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) return;

            session.setIsDelete(true);
            sessionRepository.save(session);
            messageRepository.deleteBySessionId(sessionId);
            log.info("✅ 删除会话[{}]成功", sessionId);
        } catch (Exception e) {
            log.error("❌ 删除会话失败：{}", e.getMessage());
        }
    }

    private AiChatSession getOrCreateSession(Long userId) {
        AiChatSession session = sessionRepository.findByUserIdAndSessionTitle(userId, "默认对话");
        if (session == null) {
            session = new AiChatSession();
            session.setUserId(userId);
            session.setSessionTitle("默认对话");
            session.setCreateTime(LocalDateTime.now());
        }
        session.setUpdateTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    private void batchSaveMessages(String sessionId, Long userId, List<ChatMessage> messageList) {
        // 查询已有消息数量，只保存新增的消息（增量保存，避免全量删除重插的数据丢失风险）
        long existingCount = messageRepository.countBySessionId(sessionId);
        if (existingCount >= messageList.size()) {
            return; // 没有新消息需要保存
        }

        // 只保存新增的消息
        List<ChatMessage> newMessages = messageList.subList((int) existingCount, messageList.size());
        List<AiChatMessage> mongoMessages = newMessages.stream().map(msg -> {
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

    private AiChatSession getSessionById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
    }

    /**
     * 新建独立会话（前端新建会话专用）
     *
     * @param userId 用户ID
     * @return 新会话ID
     */
    public SessionListVO createNewSession(Long userId) {
        AiChatSession session = new AiChatSession();
        // 不手动设置ID，MongoDB自动生成ObjectId
        session.setUserId(userId);
        session.setSessionTitle("新会话 " + String.format("%tR", new Date()));
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDelete(false);
        // 保存会话
        AiChatSession savedSession = sessionRepository.save(session);

        // 直接转换为VO返回，不用查列表！
        SessionListVO vo = new SessionListVO();
        BeanUtils.copyProperties(savedSession, vo);
        vo.setSessionId(savedSession.getId());

        return vo;
    }
}
