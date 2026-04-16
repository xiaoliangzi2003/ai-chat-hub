package com.quantum.ai.chataihub.entity.ai.chat;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

/**
 * AI 会话主表
 *
 * @author xuhaodong
 * @date 2026/4/16 14:45
 */
@Data
@Document("ai_chat_session") // MongoDB 集合名
public class AiChatSession {
    @Id
    private String id;          // MongoDB 主键

    @Field(targetType = FieldType.INT64)
    private Long userId;        // 用户ID（关联你的用户系统）
    private String sessionTitle;// 会话标题（默认：AI 对话）
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 最后更新时间
    private Boolean isDelete = false; // 软删除
}
