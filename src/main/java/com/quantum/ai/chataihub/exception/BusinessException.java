package com.quantum.ai.chataihub.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author xuhaodong
 * @date 2026/4/15 17:48
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
