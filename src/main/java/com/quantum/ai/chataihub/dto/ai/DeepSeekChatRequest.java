package com.quantum.ai.chataihub.dto.ai;

import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import com.quantum.ai.chataihub.entity.ai.tool.Tool;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 对话请求
 *
 * @author xuhaodong
 * @date 2026/4/16 9:11
 */
@Data
@Schema(name = "DeepSeek对话请求")
public class DeepSeekChatRequest {

    @Schema(description = "消息列表（支持system/user/assistant/tool）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ChatMessage> messages;

    @Schema(description = "模型名称", example = "deepseek-chat")
    private String model;

    @Schema(description = "思考模式")
    private Thinking thinking = new Thinking();

    @Schema(description = "频率惩罚", example = "0")
    private Double frequency_penalty;

    @Schema(description = "最大token数", example = "4096")
    private Integer max_tokens;

    @Schema(description = "存在惩罚", example = "0")
    private Double presence_penalty;

    @Schema(description = "响应格式")
    private ResponseFormat response_format = new ResponseFormat();

    @Schema(description = "停止词")
    private Object stop;

    @Schema(description = "是否流式输出", example = "false")
    private Boolean stream;

    @Schema(description = "流选项")
    private Object stream_options;

    @Schema(description = "温度", example = "1")
    private Double temperature;

    @Schema(description = "核采样", example = "1")
    private Double top_p;

    @Schema(description = "可用工具列表")
    private List<Tool> tools;

    @Schema(description = "工具选择策略", example = "none")
    private String tool_choice;

    @Schema(description = "是否返回概率", example = "false")
    private Boolean logprobs;

    @Schema(description = "Top概率")
    private Object top_logprobs;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "对话模式：quick=快速响应(默认)，think=深度思考", example = "quick")
    private String mode = "quick";

    @Data
    public static class Thinking {
        private String type = "disabled";
    }

    @Data
    public static class ResponseFormat {
        private String type = "text";
    }
}
