package com.quantum.ai.chataihub.entity.ai.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author xuhaodong
 * @date 2026/4/16 9:31
 */
@Data
@Schema(name = "工具函数")
public class ToolFunction {

    @Schema(description = "函数名称")
    private String name;

    @Schema(description = "函数参数（JSON字符串）")
    private String arguments;
}
