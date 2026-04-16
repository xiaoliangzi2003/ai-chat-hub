package com.quantum.ai.chataihub.controller.auth;

import com.quantum.ai.chataihub.constant.Result;
import com.quantum.ai.chataihub.dto.auth.CodeLoginRequest;
import com.quantum.ai.chataihub.dto.auth.PasswordLoginRequest;
import com.quantum.ai.chataihub.dto.auth.RegisterRequest;
import com.quantum.ai.chataihub.dto.auth.SendCodeRequest;
import com.quantum.ai.chataihub.entity.sys.SysUser;
import com.quantum.ai.chataihub.service.auth.AuthService;
import com.quantum.ai.chataihub.util.IpUtil;
import com.quantum.ai.chataihub.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final IpUtil ipUtil;

    // 发送邮箱验证码
    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendCodeRequest request, HttpServletRequest httpServletRequest) {
        String ip = ipUtil.getClientIp(httpServletRequest);
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
    public Result<Map<String, Object>> loginByPassword(@Valid @RequestBody PasswordLoginRequest request, HttpServletRequest httpRequest) {
        // 获取真实公网IP
        String clientIp = ipUtil.getClientIp(httpRequest);
        //  传递IP给Service做校验
        String token = authService.loginByPassword(request.getEmail(), request.getPassword(), clientIp);

        Long userId = jwtUtil.getUserIdFromToken(token);
        SysUser user = authService.getCurrentUser(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        data.put("userName", user.getEmail());
        return Result.ok(data);
    }

    // 邮箱验证码登录
    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@Valid @RequestBody CodeLoginRequest request, HttpServletRequest httpRequest) {
        // 获取真实公网IP
        String clientIp = ipUtil.getClientIp(httpRequest);
        //  传递IP给Service做校验
        String token = authService.loginByCode(request.getEmail(), request.getCode(), clientIp);

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
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        SysUser user = authService.getCurrentUser(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("email", user.getEmail());
        data.put("createTime", user.getCreateTime());
        return Result.ok(data);
    }

    // 登出功能
    @Operation(summary = "登出功能")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return Result.ok();
    }
}
