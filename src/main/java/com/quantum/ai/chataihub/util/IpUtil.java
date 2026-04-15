package com.quantum.ai.chataihub.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP地址工具类
 *
 * @author xuhaodong
 * @date 2026/4/15 18:39
 */
@Slf4j
@Component
public class IpUtil {

    // 定义优先读取的请求头
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",   // 最常用：反向代理（Nginx/Apache）
            "X-Real-IP",         // Nginx 直接配置的真实IP
            "Proxy-Client-IP",   // 代理服务器IP
            "WL-Proxy-Client-IP",// WebLogic代理
            "HTTP_CLIENT_IP",    // 客户端IP
            "HTTP_X_FORWARDED_FOR" // 转发IP
    };

    /**
     * 获取客户端真实公网IP
     */
    public String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 遍历所有IP请求头，按优先级获取
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            // 有效IP：不为空、不是unknown
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        // 如果所有代理头都没有，才用原生方法
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多级代理：X-Forwarded-For 格式 = 客户端IP,代理1IP,代理2IP
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 本地调试特殊处理：127.0.0.1 替换为真实局域网/公网IP
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.error("获取本地IP失败", e);
            }
        }

        return ip;
    }
}
