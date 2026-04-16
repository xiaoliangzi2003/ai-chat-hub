package com.quantum.ai.chataihub.entity.ai.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author xuhaodong
 * @date 2026/4/16 9:42
 */
@Data
class ToolFunctionDefinition {
    @Schema(description = "函数名称")
    private String name;
    @Schema(description = "函数描述")
    private String description;
    @Schema(description = "函数参数")
    private Object parameters;
}
