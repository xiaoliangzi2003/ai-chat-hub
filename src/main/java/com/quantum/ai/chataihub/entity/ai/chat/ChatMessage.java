package com.quantum.ai.chataihub.entity.ai.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quantum.ai.chataihub.entity.ai.tool.ToolCall;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 对话消息实体（支持system/user/assistant/tool四种角色）
 *
 * @author xuhaodong
 * @date 2026/4/16 9:15
 */
@Data
@Schema(name = "对话消息")
public class ChatMessage {

    /**
     * 角色枚举：system / user / assistant / tool
     */
    @Schema(description = "消息角色", requiredMode = Schema.RequiredMode.REQUIRED,
            examples = {"system", "user", "assistant", "tool"})
    private String role;

    @Schema(description = "消息内容", example = "you are a helpful assistant")
    private String content;

    @Schema(description = "工具调用ID（仅tool角色使用）")
    @JsonProperty("tool_call_id")
    private String toolCallId;

    @Schema(description = "模型发起的工具调用（仅assistant角色使用）")
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
}
