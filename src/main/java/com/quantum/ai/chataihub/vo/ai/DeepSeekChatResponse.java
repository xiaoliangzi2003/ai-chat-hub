package com.quantum.ai.chataihub.vo.ai;

import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author xuhaodong
 * @date 2026/4/16 9:11
 */
@Data
@Schema(name = "DeepSeek对话响应")
public class DeepSeekChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private String system_fingerprint;

    @Data
    public static class Choice {
        private Integer index;
        private ChatMessage message;
        private Object logprobs;
        private String finish_reason;
    }

    @Data
    public static class Usage {
        private Integer prompt_tokens;
        private Integer completion_tokens;
        private Integer total_tokens;
        private PromptTokensDetails prompt_tokens_details;
        private Integer prompt_cache_hit_tokens;
        private Integer prompt_cache_miss_tokens;
    }

    @Data
    public static class PromptTokensDetails {
        private Integer cached_tokens;
    }
}
