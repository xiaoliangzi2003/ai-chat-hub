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
import okhttp3.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

            // 调用AI接口
            Request okRequest = buildRequest(request);
            try (Response response = client.newCall(okRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(ResultCode.MODEL_CALL_ERROR, "DeepSeek接口调用失败");
                }

                assert response.body() != null;
                DeepSeekChatResponse aiResp = objectMapper.readValue(response.body().string(), DeepSeekChatResponse.class);

                // 保存AI回复
                ChatMessage assistantMsg = aiResp.getChoices().getFirst().getMessage();
                history.add(assistantMsg);

                // 更新Redis缓存
                redisTemplate.opsForValue().set(redisKey, history, RedisKeys.LOGIN_IP_EXPIRE, TimeUnit.MINUTES);

                // 异步持久化到MongoDB
                aiChatAsyncSaveService.asyncSaveChatRecord(userId, sessionId, history);

                return aiResp;
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL, "DeepSeek对话异常：" + e.getMessage());
        }
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
