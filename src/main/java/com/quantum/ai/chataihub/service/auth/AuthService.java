package com.quantum.ai.chataihub.service.auth;

import com.quantum.ai.chataihub.constant.RedisKeys;
import com.quantum.ai.chataihub.constant.ResultCode;
import com.quantum.ai.chataihub.entity.SysUser;
import com.quantum.ai.chataihub.exception.BusinessException;
import com.quantum.ai.chataihub.service.user.SysUserService;
import com.quantum.ai.chataihub.util.CodeUtil;
import com.quantum.ai.chataihub.util.JwtUtil;
import com.quantum.ai.chataihub.util.MailUtil;
import com.quantum.ai.chataihub.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 认证服务
 *
 * @author xuhaodong
 * @date 2026/4/15 17:57
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserService sysUserService;
    private final RedisUtil redisUtil;
    private final MailUtil mailUtil;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-max-retry:5}")
    private int maxRetry;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // 1. 发送邮箱验证码
    public void sendEmailCode(String email) {
        // 校验邮箱格式
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ResultCode.EMAIL_FORMAT_ERROR, "邮箱格式错误");
        }

        String freqKey = RedisKeys.EMAIL_FREQ_PREFIX + email;
        // 检查发送频率
        if (redisUtil.hasKey(freqKey)) {
            throw new BusinessException(ResultCode.CODE_SEND_TOO_FREQUENT, "验证码发送过于频繁，请60秒后重试");
        }

        String code = CodeUtil.generateCode();
        // 发送邮件
        mailUtil.sendVerificationCode(email, code);

        // 存储验证码 (5分钟)
        String codeKey = RedisKeys.EMAIL_CODE_PREFIX + email;
        redisUtil.set(codeKey, code, 300, TimeUnit.SECONDS);
        // 设置频率限制 (60秒)
        redisUtil.set(freqKey, "1", 60, TimeUnit.SECONDS);
    }

    // 2. 用户注册
    public String register(String email, String password, String code) {
        // 校验邮箱格式
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ResultCode.EMAIL_FORMAT_ERROR, "邮箱格式错误");
        }

        // 检查邮箱是否已注册
        SysUser existUser = sysUserService.getByEmail(email);
        if (existUser != null) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_REGISTERED, "该邮箱已被注册");
        }

        // 校验验证码
        validateCode(email, code);

        // 创建用户
        SysUser user = new SysUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setErrorCount(0);
        user.setStatus(0);
        sysUserService.save(user);

        // 删除验证码
        redisUtil.delete(RedisKeys.EMAIL_CODE_PREFIX + email);

        // 生成并返回 Token
        String token = jwtUtil.generateToken(user.getId());
        // 存储 Token 到 Redis (用于后续校验)
        redisUtil.set(RedisKeys.USER_TOKEN_PREFIX + user.getId(), token, 7200, TimeUnit.SECONDS);
        return token;
    }

    // 3. 密码登录
    public String loginByPassword(String email, String password, String clientIp) {
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        // 检查账号是否锁定
        if (sysUserService.isAccountLocked(user)) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "账号已被锁定，请1小时后重试或使用验证码登录");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 增加错误次数
            sysUserService.incrementErrorCount(user.getId());
            int leftCount = maxRetry - (user.getErrorCount() + 1);
            if (leftCount <= 0) {
                sysUserService.lockAccount(user.getId());
                throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "密码错误次数过多，账号已被锁定，请1小时后重试");
            }
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "密码错误，剩余" + leftCount + "次机会");
        }

        // IP重复登录校验
        checkLoginIp(user.getId(), clientIp);

        // 登录成功，重置错误次数
        sysUserService.resetErrorCount(user.getId());

        // 保存登录IP
        saveLoginIp(user.getId(), clientIp);

        return generateAndStoreToken(user.getId());
    }

    // 4. 验证码登录
    public String loginByCode(String email, String code, String clientIp) {
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        // 校验验证码
        validateCode(email, code);

        // IP重复登录校验
        checkLoginIp(user.getId(), clientIp);

        // 登录成功，重置错误次数并解锁
        sysUserService.resetErrorCount(user.getId());

        // 删除验证码
        redisUtil.delete(RedisKeys.EMAIL_CODE_PREFIX + email);

        // 保存登录IP
        saveLoginIp(user.getId(), clientIp);

        return generateAndStoreToken(user.getId());
    }

    // 5. 获取当前用户信息
    public SysUser getCurrentUser(Long userId) {
        return sysUserService.getById(userId);
    }

    // 辅助方法：校验验证码
    private void validateCode(String email, String code) {
        String key = RedisKeys.EMAIL_CODE_PREFIX + email;
        String storedCode = redisUtil.get(key);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(ResultCode.CODE_ERROR_OR_EXPIRED, "验证码错误或已过期");
        }
    }

    // 生成并存储 Token
    private String generateAndStoreToken(Long userId) {
        String token = jwtUtil.generateToken(userId);
        redisUtil.set(RedisKeys.USER_TOKEN_PREFIX + userId, token, 7200, TimeUnit.SECONDS);
        return token;
    }

    // 校验 Token 是否有效（用于拦截器）
    public boolean validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        String storedToken = redisUtil.get(RedisKeys.USER_TOKEN_PREFIX + userId);
        return token.equals(storedToken);
    }

    public Long getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 校验IP：同一用户+同一IP已登录，拒绝重复登录
     */
    private void checkLoginIp(Long userId, String currentIp) {
        String key = RedisKeys.LOGIN_IP_KEY + userId;
        String loggedIp = redisUtil.get(key);

        // 已登录 + IP相同 → 拦截
        if (loggedIp != null && loggedIp.equals(currentIp)) {
            throw new BusinessException(ResultCode.FAIL, "当前IP已登录，请勿重复登录！");
        }
    }

    /**
     * 保存登录IP到Redis
     */
    private void saveLoginIp(Long userId, String ip) {
        String key = RedisKeys.LOGIN_IP_KEY + userId;
        redisUtil.set(key, ip, RedisKeys.LOGIN_IP_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 登出功能
     */
    public void logout(String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        redisUtil.delete(RedisKeys.USER_TOKEN_PREFIX + userId);
        redisUtil.delete(RedisKeys.LOGIN_IP_KEY + userId);
    }
}
