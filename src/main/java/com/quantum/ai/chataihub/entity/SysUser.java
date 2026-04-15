package com.quantum.ai.chataihub.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户核心信息表(SysUser)表实体类
 *
 * @author quantum
 * @since 2026-04-15 17:26:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {
    /**
     * 用户 ID（自增主键）
     */

    private Long id;
    /**
     * 用户邮箱（唯一）
     */

    private String email;
    /**
     * 加密后的密码（BCrypt 加密）
     */

    private String password;
    /**
     * 密码错误次数
     */

    private Integer errorCount;
    /**
     * 账号锁定时间
     */

    private Date lockTime;
    /**
     * 用户状态：0 正常，1 锁定
     */

    private Integer status;
    /**
     * 创建时间
     */

    private Date createTime;
    /**
     * 更新时间
     */

    private Date updateTime;

}
