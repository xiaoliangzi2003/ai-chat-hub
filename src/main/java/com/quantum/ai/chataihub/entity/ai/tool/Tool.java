package com.quantum.ai.chataihub.entity.ai.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author xuhaodong
 * @date 2026/4/16 9:42
 */
@Data
@Schema(name = "工具定义")
public class Tool {
    @Schema(description = "工具类型", example = "function")
    private String type = "function";

    @Schema(description = "函数定义")
    private ToolFunctionDefinition function;
}
