package com.quantum.ai.chataihub.entity.ai.chat;

import com.quantum.ai.chataihub.entity.ai.tool.ToolCall;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * AI 对话消息明细表
 *
 * @author xuhaodong
 * @date 2026/4/16 14:46
 */
@Data
@Document("ai_chat_message")
public class AiChatMessage {
    @Id
    private String id;
    private String sessionId;       // 关联会话ID
    private Long userId;            // 用户ID
    private String role;            // 角色：system/user/assistant/tool
    private String content;         // 消息内容
    private List<ToolCall> toolCalls;// 工具调用
    private String toolCallId;      // 工具ID
    private Integer tokens;         // Token 消耗
    private Date createTime;        // 消息时间
}
