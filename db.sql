CREATE TABLE `sys_user`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户 ID（自增主键）',
    `email`       varchar(100) NOT NULL COMMENT '用户邮箱（唯一）',
    `password`    varchar(255) NOT NULL COMMENT '加密后的密码（BCrypt 加密）',
    `error_count` int          NOT NULL DEFAULT 0 COMMENT '密码错误次数',
    `lock_time`   datetime              DEFAULT NULL COMMENT '账号锁定时间',
    `status`      tinyint(1)   NOT NULL DEFAULT 0 COMMENT '用户状态：0 正常，1 锁定',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 主键约束
    PRIMARY KEY (`id`),
    -- 邮箱唯一索引（防止重复注册）
    UNIQUE KEY `uk_sys_user_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户核心信息表';