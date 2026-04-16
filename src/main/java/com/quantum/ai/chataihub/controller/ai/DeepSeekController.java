package com.quantum.ai.chataihub.controller.ai;

import com.quantum.ai.chataihub.constant.Result;
import com.quantum.ai.chataihub.dto.ai.DeepSeekChatRequest;
import com.quantum.ai.chataihub.service.ai.DeepSeekService;
import com.quantum.ai.chataihub.vo.ai.DeepSeekChatResponse;
import com.quantum.ai.chataihub.vo.ai.SessionDetailVO;
import com.quantum.ai.chataihub.vo.ai.SessionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "清空指定会话记忆")
    @PostMapping("/clear")
    public Result<String> clearMemory(
            @RequestParam String sessionId,
            HttpServletRequest request) {
        deepSeekService.clearMemory(sessionId, request);
        return Result.ok("清空成功");
    }

    @Operation(summary = "获取用户会话列表")
    @GetMapping("/session/list")
    public Result<List<SessionListVO>> getSessionList(HttpServletRequest request) {
        return Result.ok(deepSeekService.getSessionList(request));
    }

    @Operation(summary = "获取会话详情（历史记录）")
    @GetMapping("/session/detail")
    public Result<SessionDetailVO> getSessionDetail(
            @RequestParam String sessionId,
            HttpServletRequest request) {
        return Result.ok(deepSeekService.getSessionDetail(sessionId, request));
    }

    // 新增：新建会话
    @Operation(summary = "新建会话")
    @PostMapping("/session/create")
    public Result<SessionListVO> createSession(HttpServletRequest request) {
        return Result.ok(deepSeekService.createSession(request));
    }
}
