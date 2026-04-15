package com.quantum.ai.chataihub.interceptor;

import com.quantum.ai.chataihub.constant.ResultCode;
import com.quantum.ai.chataihub.exception.BusinessException;
import com.quantum.ai.chataihub.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证拦截器
 *
 * @author xuhaodong
 * @date 2026/4/15 18:05
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或Token已过期");
        }
        String token = authHeader.substring(7);
        if (!authService.validateToken(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或Token已过期");
        }
        // 可将 userId 存入 request 属性供后续使用
        Long userId = authService.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
