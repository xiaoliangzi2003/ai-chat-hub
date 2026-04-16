package com.quantum.ai.chataihub.constant;

/**
 * @author xuhaodong
 * @date 2026/4/15 17:54
 */
public class RedisKeys {
    // 邮箱验证码
    public static final String EMAIL_CODE_PREFIX = "email:code:";
    // 邮箱验证码发送频率
    public static final String EMAIL_FREQ_PREFIX = "email:freq:";
    // 用户登录凭证
    public static final String USER_TOKEN_PREFIX = "user:token:";
    // 登录IP
    public static final String LOGIN_IP_KEY = "login:ip:";
    // 登录IP过期时间（7天）
    public static final long LOGIN_IP_EXPIRE = 60 * 60 * 24 * 7;

    // deepseek对话记忆缓存key
    public static final String DEEPSEEK_CACHE_KEY = "deepseek:memory:";
    public static final long DEEPSEEK_CACHE_EXPIRE = 30;

}
