package com.quantum.ai.chataihub.vo.ai;

import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import lombok.Data;

import java.util.List;

/**
 * 对话详情
 *
 * @author xuhaodong
 * @date 2026/4/16 15:32
 */
@Data
public class SessionDetailVO {
    private String sessionId;
    private String sessionTitle;
    private List<ChatMessage> historyMessages; // 历史对话记录
}
