package com.quantum.ai.chataihub.controller.ai;

import com.quantum.ai.chataihub.constant.Result;
import com.quantum.ai.chataihub.dto.ai.DeepSeekChatRequest;
import com.quantum.ai.chataihub.service.ai.DeepSeekService;
import com.quantum.ai.chataihub.vo.ai.DeepSeekChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DeepSeek AI
 *
 * @author xuhaodong
 * @date 2026/4/16 9:07
 */
@Tag(name = "DeepSeek AI")
@RestController
@RequestMapping("/api/ai/deepseek")
@RequiredArgsConstructor
public class DeepSeekController {

    private final DeepSeekService deepSeekService;

    @Operation(summary = "DeepSeek对话(带记忆）")
    @RequestMapping("/chat")
    public Result<DeepSeekChatResponse> chat(@RequestBody DeepSeekChatRequest request, HttpServletRequest httpServletRequest) {
        return Result.ok(deepSeekService.chat(request, httpServletRequest));
    }

    @Operation(summary = "清空对话记忆")
    @PostMapping("/clear")
    public Result<String> clearMemory(HttpServletRequest request) {
        deepSeekService.clearMemory(request);
        return Result.ok("清空成功");
    }
}
