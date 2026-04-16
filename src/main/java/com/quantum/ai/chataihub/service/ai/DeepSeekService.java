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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
            // 获取userId
            String token = httpServletRequest.getHeader("Authorization").replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            String key = RedisKeys.DEEPSEEK_CACHE_KEY + userId;

            // 从请求中取出前端传递的最新消息
            List<ChatMessage> newMessages = request.getMessages();
            if (newMessages == null || newMessages.isEmpty()) {
                throw new BusinessException(ResultCode.NO_MESSAGE, "请发送消息");
            }
            ChatMessage lastUserMessage = newMessages.getLast();

            // 从Redis中读取历史记忆
            List<ChatMessage> history = (List<ChatMessage>) redisTemplate.opsForValue().get(key);
            if (history == null) {
                history = new ArrayList<>();
                // 首次对话：自动加入系统提示
                ChatMessage system = new ChatMessage();
                system.setRole(DeepSeekConstants.ROLE_SYSTEM);
                system.setContent(DeepSeekConstants.SYSTEM_PROMPT);
                history.add(system);
            }

            // 追加最新用户消息
            history.add(lastUserMessage);

            // 把完整历史消息设置到请求中
            request.setMessages(history);

            // 填充默认配置（从yml读取，无需手动传）
            fillDefaultConfig(request);

            // 调用DeekSeek接口
            Request okRequest = buildRequest(request);

            // 发送请求并解析响应
            try (Response response = client.newCall(okRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(ResultCode.MODEL_CALL_ERROR, "DeepSeek接口调用失败：");
                }

                assert response.body() != null;
                DeepSeekChatResponse aiResp = objectMapper.readValue(response.body().string(), DeepSeekChatResponse.class);

                // 保存AI回复到记忆
                ChatMessage assistantMsg = aiResp.getChoices().getFirst().getMessage();
                history.add(assistantMsg);
                redisTemplate.opsForValue().set(key, history, RedisKeys.LOGIN_IP_EXPIRE, TimeUnit.MINUTES);

                aiChatAsyncSaveService.asyncSaveChatRecord(userId, history);

                return aiResp;
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL, "DeepSeek对话异常：" + e.getMessage());
        }
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
     * 填充默认配置（从yml读取，简化前端传参）
     */
    private void fillDefaultConfig(DeepSeekChatRequest request) {
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

    public void clearMemory(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        redisTemplate.delete(RedisKeys.DEEPSEEK_CACHE_KEY + userId);
        // 异步删除
        aiChatAsyncSaveService.asyncDeleteSession(userId);
    }
}
