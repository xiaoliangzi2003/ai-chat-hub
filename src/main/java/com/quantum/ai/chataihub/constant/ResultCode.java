package com.quantum.ai.chataihub.constant;

/**
 * @author xuhaodong
 * @date 2026/4/15 17:46
 */
public class ResultCode {
    /**
     * 常规HTTP响应码
     *
     */
    public static final int SUCCESS = 200;
    public static final int FAIL = 500;
    public static final int NOT_FOUND = 404;
    public static final int BAD_REQUEST = 400;
    public static final int FORBIDDEN = 403;
    public static final int UNAUTHORIZED = 401;

    /**
     * 注册登录响应异常码
     *
     */
    public static final int EMAIL_FORMAT_ERROR = 10001;
    public static final int CODE_SEND_TOO_FREQUENT = 10002;
    public static final int CODE_ERROR_OR_EXPIRED = 10003;
    public static final int PASSWORD_ERROR = 10004;
    public static final int ACCOUNT_LOCKED = 10005;
    public static final int EMAIL_ALREADY_REGISTERED = 10006;
    public static final int USER_NOT_EXIST = 10007;

}
