package com.quantum.ai.chataihub.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xuhaodong
 * @date 2026/4/16 15:32
 */
@Data
public class SessionListVO {
    private String sessionId;      // 会话ID
    private String sessionTitle;   // 会话标题
    private LocalDateTime createTime;       // 创建时间
    private LocalDateTime updateTime;       // 更新时间
}
