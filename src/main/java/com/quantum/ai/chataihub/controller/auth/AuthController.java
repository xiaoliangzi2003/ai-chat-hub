package com.quantum.ai.chataihub.controller.auth;

import com.quantum.ai.chataihub.constant.Result;
import com.quantum.ai.chataihub.constant.ResultCode;
import com.quantum.ai.chataihub.dto.auth.CodeLoginRequest;
import com.quantum.ai.chataihub.dto.auth.PasswordLoginRequest;
import com.quantum.ai.chataihub.dto.auth.RegisterRequest;
import com.quantum.ai.chataihub.dto.auth.SendCodeRequest;
import com.quantum.ai.chataihub.entity.SysUser;
import com.quantum.ai.chataihub.exception.BusinessException;
import com.quantum.ai.chataihub.service.auth.AuthService;
import com.quantum.ai.chataihub.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xuhaodong
 * @date 2026/4/15 17:59
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "登录注册认证接口")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // 发送邮箱验证码
    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendEmailCode(request.getEmail());
        return Result.ok();
    }

    // 用户注册
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request.getEmail(), request.getPassword(), request.getCode());
        Long userId = jwtUtil.getUserIdFromToken(token);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        return Result.ok(data);
    }

    // 邮箱密码登录
    @Operation(summary = "邮箱密码登录")
    @PostMapping("/login/password")
    public Result<Map<String, Object>> loginByPassword(@Valid @RequestBody PasswordLoginRequest request) {
        String token = authService.loginByPassword(request.getEmail(), request.getPassword());
        Long userId = jwtUtil.getUserIdFromToken(token);
        SysUser user = authService.getCurrentUser(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        data.put("userName", user.getEmail()); // 示例，可扩展昵称字段
        return Result.ok(data);
    }

    // 邮箱验证码登录
    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@Valid @RequestBody CodeLoginRequest request) {
        String token = authService.loginByCode(request.getEmail(), request.getCode());
        Long userId = jwtUtil.getUserIdFromToken(token);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        return Result.ok(data);
    }

    // 获取当前用户信息
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或Token已过期");
        }
        String token = authHeader.substring(7);
        if (!authService.validateToken(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或Token已过期");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        SysUser user = authService.getCurrentUser(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("email", user.getEmail());
        data.put("createTime", user.getCreateTime());
        return Result.ok(data);
    }
}
