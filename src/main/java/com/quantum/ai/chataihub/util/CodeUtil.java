package com.quantum.ai.chataihub.util;

import java.util.Random;

/**
 * 验证码生成工具类
 *
 * @author xuhaodong
 * @date 2026/4/15 17:53
 */
public class CodeUtil {
    private static final Random random = new Random();

    public static String generateCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
}
