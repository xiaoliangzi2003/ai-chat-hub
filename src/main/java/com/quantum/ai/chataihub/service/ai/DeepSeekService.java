package com.quantum.ai.chataihub.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantum.ai.chataihub.config.DeepSeekProperties;
import com.quantum.ai.chataihub.constant.DeepSeekConstants;
import com.quantum.ai.chataihub.constant.RedisKeys;
import com.quantum.ai.chataihub.constant.ResultCode;
import com.quantum.ai.chataihub.dto.ai.DeepSeekChatRequest;
import com.quantum.ai.chataihub.entity.ai.chat.ChatMessage;
import com.quantum.ai.chataihub.exception.BusinessException;
import com.quantum.ai.chataihub.util.JwtUtil;
import com.quantum.ai.chataihub.vo.ai.DeepSeekChatResponse;
import com.quantum.ai.chataihub.vo.ai.SessionDetailVO;
import com.quantum.ai.chataihub.vo.ai.SessionListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI服务
 *
 * @author xuhaodong
 * @date 2026/4/16 9:17
 */
@Tag(name = "DeepSeek AI服务")
@Service
@RequiredArgsConstructor
@Slf4j
public class DeepSeekService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)    // 连接超时 10秒
            .readTimeout(60, TimeUnit.SECONDS)       // 读取超时 60秒
            .writeTimeout(60, TimeUnit.SECONDS)      // 写入超时 60秒
            .build();

    private final ObjectMapper objectMapper;
    private final DeepSeekProperties deepSeekProperties;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiChatAsyncSaveService aiChatAsyncSaveService;

    public DeepSeekChatResponse chat(DeepSeekChatRequest request, HttpServletRequest httpServletRequest) {
        try {
            // 获取用户ID
            String token = httpServletRequest.getHeader("Authorization").replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 处理会话ID（无则自动创建默认会话）
            String sessionId = request.getSessionId();
            if (!StringUtils.hasText(sessionId)) {
                sessionId = aiChatAsyncSaveService.createDefaultSession(userId);
            }

            // Redis Key：按会话ID存储（多会话隔离）
            String redisKey = RedisKeys.DEEPSEEK_CACHE_KEY + sessionId;

            // 校验用户消息
            List<ChatMessage> newMessages = request.getMessages();
            if (newMessages == null || newMessages.isEmpty()) {
                throw new BusinessException(ResultCode.NO_MESSAGE, "请发送消息");
            }
            ChatMessage lastUserMessage = newMessages.getLast();

            // 读取会话历史（Redis优先，无则从MongoDB加载）
            List<ChatMessage> history = (List<ChatMessage>) redisTemplate.opsForValue().get(redisKey);
            if (history == null) {
                history = aiChatAsyncSaveService.getSessionMessages(sessionId);
                if (history == null || history.isEmpty()) {
                    history = new ArrayList<>();
                    // 首次对话：添加系统提示
                    ChatMessage system = new ChatMessage();
                    system.setRole(DeepSeekConstants.ROLE_SYSTEM);
                    system.setContent(DeepSeekConstants.SYSTEM_PROMPT);
                    history.add(system);
                }
            }

            // 追加用户消息
            history.add(lastUserMessage);
            request.setMessages(history);

            // 填充默认配置
            fillDefaultConfig(request);

            // 调用AI接口（含重试逻辑）
            DeepSeekChatResponse aiResp = callWithRetry(request, 3);

            // 保存AI回复
            ChatMessage assistantMsg = aiResp.getChoices().getFirst().getMessage();
            history.add(assistantMsg);

            // 更新Redis缓存
            redisTemplate.opsForValue().set(redisKey, history, RedisKeys.DEEPSEEK_CACHE_EXPIRE, TimeUnit.MINUTES);

            // 异步持久化到MongoDB
            aiChatAsyncSaveService.asyncSaveChatRecord(userId, sessionId, history);

            return aiResp;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL, "DeepSeek对话异常：" + e.getMessage());
        }
    }

    /**
     * 流式对话（SSE）
     */
    @SuppressWarnings("unchecked")
    public SseEmitter chatStream(DeepSeekChatRequest request, HttpServletRequest httpServletRequest) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2分钟超时

        try {
            // 获取用户ID
            String tokenStr = httpServletRequest.getHeader("Authorization").replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(tokenStr);

            // 处理会话ID
            String sessionId = request.getSessionId();
            if (!StringUtils.hasText(sessionId)) {
                sessionId = aiChatAsyncSaveService.createDefaultSession(userId);
            }

            String redisKey = RedisKeys.DEEPSEEK_CACHE_KEY + sessionId;

            // 校验用户消息
            List<ChatMessage> newMessages = request.getMessages();
            if (newMessages == null || newMessages.isEmpty()) {
                emitter.completeWithError(new BusinessException(ResultCode.NO_MESSAGE, "请发送消息"));
                return emitter;
            }
            ChatMessage lastUserMessage = newMessages.getLast();

            // 读取会话历史
            List<ChatMessage> history = (List<ChatMessage>) redisTemplate.opsForValue().get(redisKey);
            if (history == null) {
                history = aiChatAsyncSaveService.getSessionMessages(sessionId);
                if (history == null || history.isEmpty()) {
                    history = new ArrayList<>();
                    ChatMessage system = new ChatMessage();
                    system.setRole(DeepSeekConstants.ROLE_SYSTEM);
                    system.setContent(DeepSeekConstants.SYSTEM_PROMPT);
                    history.add(system);
                }
            }

            history.add(lastUserMessage);
            request.setMessages(history);

            // 填充默认配置并启用流式
            fillDefaultConfig(request);
            request.setStream(true);

            // 异步执行流式请求
            final List<ChatMessage> finalHistory = history;
            final String finalSessionId = sessionId;
            final Long finalUserId = userId;
            final String finalRedisKey = redisKey;

            Request okRequest = buildRequest(request);
            client.newCall(okRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data("网络异常：" + e.getMessage()));
                    } catch (Exception ignored) {
                    }
                    emitter.completeWithError(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    StringBuilder fullContent = new StringBuilder();
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "";
                            BusinessException ex = parseApiError(response.code(), errorBody);
                            emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
                            emitter.complete();
                            return;
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) continue;
                                if (!line.startsWith("data: ")) continue;

                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                    break;
                                }

                                // 解析 SSE chunk，提取 delta content
                                try {
                                    var node = objectMapper.readTree(data);
                                    var choices = node.get("choices");
                                    if (choices != null && choices.isArray() && !choices.isEmpty()) {
                                        var delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String content = delta.get("content").asText();
                                            if (content != null && !content.isEmpty()) {
                                                fullContent.append(content);
                                                emitter.send(SseEmitter.event().data(content));
                                            }
                                        }
                                    }
                                } catch (Exception parseEx) {
                                    log.warn("解析SSE chunk失败: {}", data);
                                }
                            }
                        }

                        // 流结束后保存完整回复
                        if (!fullContent.isEmpty()) {
                            ChatMessage assistantMsg = new ChatMessage();
                            assistantMsg.setRole(DeepSeekConstants.ROLE_ASSISTANT);
                            assistantMsg.setContent(fullContent.toString());
                            finalHistory.add(assistantMsg);

                            redisTemplate.opsForValue().set(finalRedisKey, finalHistory,
                                    RedisKeys.DEEPSEEK_CACHE_EXPIRE, TimeUnit.MINUTES);
                            aiChatAsyncSaveService.asyncSaveChatRecord(finalUserId, finalSessionId, finalHistory);
                        }

                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE流处理异常", e);
                        emitter.completeWithError(e);
                    }
                }
            });
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // 获取用户会话列表
    public List<SessionListVO> getSessionList(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        return aiChatAsyncSaveService.getSessionListByUserId(userId);
    }

    // 获取会话详情（历史消息）
    public SessionDetailVO getSessionDetail(String sessionId, HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        return aiChatAsyncSaveService.getSessionDetail(sessionId, userId);
    }


    private Request buildRequest(DeepSeekChatRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
            return new Request.Builder()
                    .url(deepSeekProperties.getBaseUrl() + DeepSeekConstants.CHAT_API)
                    .post(body)
                    .header("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL, "DeepSeek对话异常：" + e.getMessage());
        }

    }

    /**
     * 调用 DeepSeek API（含指数退避重试，针对 429/503 等暂时性错误）
     */
    private DeepSeekChatResponse callWithRetry(DeepSeekChatRequest request, int maxRetries) {
        Request okRequest = buildRequest(request);
        int retries = 0;
        while (true) {
            try (Response response = client.newCall(okRequest).execute()) {
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    return objectMapper.readValue(responseBody, DeepSeekChatResponse.class);
                }

                // 可重试的状态码: 429 (限流), 503 (服务不可用), 502 (网关错误)
                if ((code == 429 || code == 502 || code == 503) && retries < maxRetries) {
                    retries++;
                    long waitMs = (long) Math.pow(2, retries) * 1000; // 指数退避: 2s, 4s, 8s
                    Thread.sleep(waitMs);
                    continue;
                }

                // 不可重试或重试耗尽，解析错误信息
                throw parseApiError(code, responseBody);
            } catch (BusinessException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ResultCode.MODEL_CALL_ERROR, "请求被中断");
            } catch (java.net.SocketTimeoutException e) {
                if (retries < maxRetries) {
                    retries++;
                    continue;
                }
                throw new BusinessException(ResultCode.MODEL_CALL_TIMEOUT, "DeepSeek接口调用超时，请稍后重试");
            } catch (Exception e) {
                throw new BusinessException(ResultCode.MODEL_CALL_ERROR, "DeepSeek接口调用失败：" + e.getMessage());
            }
        }
    }

    /**
     * 解析 DeepSeek API 错误响应
     */
    private BusinessException parseApiError(int httpCode, String responseBody) {
        try {
            var errorNode = objectMapper.readTree(responseBody);
            if (errorNode.has("error")) {
                var error = errorNode.get("error");
                String type = error.has("type") ? error.get("type").asText() : "unknown";
                String message = error.has("message") ? error.get("message").asText() : "未知错误";

                if ("authentication_error".equals(type)) {
                    return new BusinessException(ResultCode.API_KEY_ERROR, "API Key 无效: " + message);
                } else if ("insufficient_balance".equals(type)) {
                    return new BusinessException(ResultCode.API_KEY_BALANCE_ERROR, "API 余额不足: " + message);
                } else if ("rate_limit_exceeded".equals(type)) {
                    return new BusinessException(ResultCode.MODEL_CALL_ERROR, "请求过于频繁，请稍后重试");
                } else {
                    return new BusinessException(ResultCode.MODEL_CALL_ERROR, "DeepSeek错误[" + type + "]: " + message);
                }
            }
        } catch (Exception ignored) {
        }
        return new BusinessException(ResultCode.MODEL_CALL_ERROR, "DeepSeek接口调用失败(HTTP " + httpCode + ")");
    }

    /**
     * 填充默认配置（从yml读取，简化前端传参）
     */
    private void fillDefaultConfig(DeepSeekChatRequest request) {
        // 切换快速响应与思考模式
        String mode = request.getMode();
        if ("think".equals(mode)) {
            // 深度思考模式：开启思考 + 切换推理模型
            request.setModel("deepseek-reasoner");
            request.setThinking(null);
        } else {
            // 快速响应模式（默认）：关闭思考 + 使用配置的标准模型
            request.setModel(deepSeekProperties.getChat().getOptions().getModel());
            request.getThinking().setType("disabled");
        }

        if (request.getModel() == null) {
            request.setModel(deepSeekProperties.getChat().getOptions().getModel());
        }
        if (request.getThinking() == null) {
            request.setThinking(new DeepSeekChatRequest.Thinking());
            request.getThinking().setType("disabled");
        }
        if (request.getFrequency_penalty() == null) request.setFrequency_penalty(0D);
        if (request.getMax_tokens() == null) request.setMax_tokens(4096);
        if (request.getPresence_penalty() == null) request.setPresence_penalty(0D);
        if (request.getResponse_format() == null) {
            request.setResponse_format(new DeepSeekChatRequest.ResponseFormat());
            request.getResponse_format().setType("text");
        }
        if (request.getStream() == null) request.setStream(false);
        if (request.getTemperature() == null) request.setTemperature(1D);
        if (request.getTop_p() == null) request.setTop_p(1D);
        if (request.getTool_choice() == null) request.setTool_choice("none");
        if (request.getLogprobs() == null) request.setLogprobs(false);
    }

    public void clearMemory(String sessionId, HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        jwtUtil.getUserIdFromToken(token); // 校验用户权限
        // 清空Redis
        redisTemplate.delete(RedisKeys.DEEPSEEK_CACHE_KEY + sessionId);
        // 异步删除数据库会话
        aiChatAsyncSaveService.asyncDeleteSessionBySessionId(sessionId);
    }

    public SessionListVO createSession(HttpServletRequest request) {
        try {
            // 解析用户ID
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 直接创建并返回VO（核心修复，不再查列表）
            return aiChatAsyncSaveService.createNewSession(userId);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL, "新建会话异常：" + e.getMessage());
        }
    }
}
