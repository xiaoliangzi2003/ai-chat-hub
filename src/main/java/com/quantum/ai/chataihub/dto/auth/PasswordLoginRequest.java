package com.quantum.ai.chataihub.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 密码登录请求
 *
 * @author xuhaodong
 * @date 2026/4/15 18:03
 */
@Data
public class PasswordLoginRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}
