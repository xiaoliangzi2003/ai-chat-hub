package com.quantum.ai.chataihub.entity.ai.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型发起的工具调用
 *
 * @author xuhaodong
 * @date 2026/4/16 9:30
 */
@Data
@Schema(name = "工具调用")
public class ToolCall {

    @Schema(description = "工具调用唯一ID")
    private String id;

    @Schema(description = "工具类型", example = "function")
    private String type;

    @Schema(description = "工具函数信息")
    private ToolFunction function;
}
