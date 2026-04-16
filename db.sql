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

CREATE TABLE `ai_model_config`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model_provider` varchar(32)  NOT NULL COMMENT '模型厂商：deepseek/openai/ollama/qwen等',
    `model_code`     varchar(64)  NOT NULL COMMENT '模型编码：deepseek-chat/deepseek-reasoner等',
    `model_name`     varchar(64)  NOT NULL COMMENT '模型名称：深度求索对话模型',
    `api_key`        varchar(255) NOT NULL COMMENT 'API密钥（加密存储）',
    `api_base_url`   varchar(255) NOT NULL COMMENT 'API请求地址',
    `api_version`    varchar(32)           DEFAULT NULL COMMENT 'API版本号',
    `max_tokens`     int                   DEFAULT 4096 COMMENT '最大token数',
    `temperature`    decimal(3, 2)         DEFAULT 0.7 COMMENT '温度系数(0-2)',
    `top_p`          decimal(3, 2)         DEFAULT 0.9 COMMENT '核采样参数',
    `is_enable`      tinyint      NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    `sort`           int                   DEFAULT 0 COMMENT '排序权重',
    `remark`         varchar(255)          DEFAULT NULL COMMENT '备注',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_code` (`model_code`, `is_deleted`) COMMENT '模型编码唯一',
    KEY `idx_provider` (`model_provider`) COMMENT '厂商索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI大模型配置表';

CREATE TABLE `chat_conversation`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`            bigint       NOT NULL COMMENT '用户ID（关联系统用户表）',
    `conversation_title` varchar(100) NOT NULL COMMENT '会话标题（自动生成/用户自定义）',
    `model_id`           bigint       NOT NULL COMMENT '使用的模型ID（关联ai_model_config）',
    `total_messages`     int          NOT NULL DEFAULT 0 COMMENT '会话总消息数',
    `total_tokens`       bigint       NOT NULL DEFAULT 0 COMMENT '会话总消耗token',
    `last_message_time`  datetime              DEFAULT NULL COMMENT '最后消息时间',
    `is_pinned`          tinyint      NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         tinyint      NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`, `is_deleted`) COMMENT '用户会话查询索引',
    KEY `idx_model_id` (`model_id`) COMMENT '模型关联索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI聊天会话表';

CREATE TABLE `chat_message`
(
    `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id`   bigint      NOT NULL COMMENT '会话ID（关联chat_conversation）',
    `user_id`           bigint      NOT NULL COMMENT '用户ID',
    `model_id`          bigint      NOT NULL COMMENT '使用的模型ID',
    `message_role`      varchar(16) NOT NULL COMMENT '消息角色：user-用户 assistant-AI system-系统提示',
    `message_content`   longtext    NOT NULL COMMENT '消息内容',
    `prompt_tokens`     int                  DEFAULT 0 COMMENT '提问token消耗',
    `completion_tokens` int                  DEFAULT 0 COMMENT '回复token消耗',
    `total_tokens`      int                  DEFAULT 0 COMMENT '总token消耗',
    `is_stream`         tinyint     NOT NULL DEFAULT 1 COMMENT '是否流式响应：0-否 1-是',
    `request_id`        varchar(128)         DEFAULT NULL COMMENT '厂商API请求ID（用于日志排查）',
    `error_msg`         varchar(512)         DEFAULT NULL COMMENT 'API错误信息（失败时存储）',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        tinyint     NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`, `is_deleted`) COMMENT '会话消息查询索引',
    KEY `idx_user_id` (`user_id`) COMMENT '用户消息索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI聊天消息表';

CREATE TABLE `chat_feedback`
(
    `id`               bigint   NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `message_id`       bigint   NOT NULL COMMENT '关联的消息ID',
    `user_id`          bigint   NOT NULL COMMENT '用户ID',
    `feedback_type`    tinyint  NOT NULL COMMENT '反馈类型：1-点赞 2-差评',
    `feedback_content` varchar(512)      DEFAULT NULL COMMENT '反馈详情/建议',
    `create_time`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`       tinyint  NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`, `is_deleted`) COMMENT '单消息单用户唯一反馈',
    KEY `idx_message_id` (`message_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI聊天消息反馈表';