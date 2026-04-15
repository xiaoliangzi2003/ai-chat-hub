# ChatAIHub 项目README

> 一个轻量、可扩展的 AI 智能聊天网站，支持对接 DeepSeek、Ollama 等主流大模型，提供实时对话、用户鉴权、聊天记录存储等核心功能。
>
>

---

## 📋 项目基础信息

| 配置项           | 版本 / 说明           |
|---------------|-------------------|
| 项目名称          | ChatAIHub         |
| SpringBoot 版本 | 3\.5\.13          |
| JDK 版本        | 26                |
| Spring AI 版本  | 1\.1\.4           |
| 项目版本          | 0\.0\.1\-SNAPSHOT |

---

## ✨ 核心功能

- ✅ 支持 DeepSeek、Ollama 双大模型对接

- ✅ WebSocket 实时聊天消息推送

- ✅ 邮箱验证注册、双模式登录（密码 / 验证码）

- ✅ 防暴力破解：密码错误 5 次自动锁定 1 小时

- ✅ 聊天记录持久化存储

- ✅ Redis 缓存会话、验证码限流

- ✅ 自动生成接口文档

- ✅ 应用健康监控

- ✅ 开发热部署

---

## 🛠️ 环境要求

- JDK 26\+

- Maven 3\.8\+

- MySQL 8\.0\+

- Redis 6\.0\+

- （可选）Ollama 本地大模型部署环境

---

## 🚀 快速启动

### 1\. 克隆项目

```bash
git clone https://github.com/your-repo/ChatAIHub.git
cd ChatAIHub
```

### 2\. 创建环境变量文件 `\.env`

在项目根目录创建 `\.env` 文件，存储敏感配置（**永不提交到 Git**）：

```env
# 大模型密钥
DEEPSEEK_APIKEY=你的DeepSeek API密钥
DEEPSEEK_BASE_URL=https://api.deepseek.com

# 数据库密码
DB_PASSWORD=你的MySQL密码
```

### 3\. 项目配置 `application\.yml`

完整规范配置（已合并所有节点，无重复）：

```yaml
# 加载环境变量
spring:
  config:
    import: optional:file:.env[.properties]

  # 应用名称
  application:
    name: chat-ai-hub

  # 启用开发环境
  profiles:
    active: dev

  # AI 大模型配置
  ai:
    deepseek:
      api-key: ${DEEPSEEK_APIKEY:}
      base-url: ${DEEPSEEK_BASE_URL:}
    ollama:
      base-url: http://localhost:11434

  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/aichat?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:}

  # Redis 缓存配置
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}

# 服务端口
server:
  port: 8080
```

### 4\. 打包运行

```bash
mvn clean package -DskipTests
java -jar target/ChatAIHub-0.0.1-SNAPSHOT.jar
```

---

## 📦 核心依赖说明

| 依赖名称                                    | 作用               | 版本       |
|-----------------------------------------|------------------|----------|
| spring\-boot\-starter\-web              | Web 服务、REST 接口开发 | 3\.5\.13 |
| spring\-boot\-starter\-websocket        | 实时聊天消息推送         | 3\.5\.13 |
| spring\-boot\-starter\-validation       | 参数校验、请求合法性验证     | 3\.5\.13 |
| spring\-boot\-starter\-actuator         | 应用健康监控           | 3\.5\.13 |
| spring\-boot\-starter\-test             | 单元测试支持           | 3\.5\.13 |
| spring\-boot\-devtools                  | 开发热部署            | 3\.5\.13 |
| spring\-ai\-starter\-model\-deepseek    | 对接 DeepSeek 大模型  | 1\.1\.4  |
| spring\-ai\-starter\-model\-ollama      | 对接 Ollama 本地大模型  | 1\.1\.4  |
| mybatis\-spring\-boot\-starter          | MyBatis 核心启动器    | 3\.0\.5  |
| mybatis\-spring\-boot\-starter\-test    | MyBatis 测试支持     | 3\.0\.5  |
| mybatis\-plus\-spring\-boot3\-starter   | 数据库 CRUD 增强框架    | 3\.5\.12 |
| mysql\-connector\-j                     | MySQL 数据库驱动      | 3\.5\.13 |
| sa\-token\-spring\-boot3\-starter       | 用户权限、登录认证        | 1\.37\.0 |
| jjwt\-api                               | JWT 令牌生成与解析      | 0\.11\.5 |
| hutool\-all                             | 通用工具类库           | 5\.8\.32 |
| fastjson2                               | JSON 序列化 / 反序列化  | 2\.0\.32 |
| lombok                                  | 代码简化工具           | 3\.5\.13 |
| springdoc\-openapi\-starter\-webmvc\-ui | 自动生成接口文档         | 2\.2\.0  |

---

## 📚 接口文档

项目启动后，访问以下地址查看接口文档：

```Plain
http://localhost:8080/swagger-ui.html
```

---

## 📁 项目结构

```Plain
ChatAIHub
├── .env                               # 环境变量文件（本地）
├── .gitignore                         # Git忽略文件
├── pom.xml                             # Maven依赖
├── src/main/java/com/cisdi/info
│   ├── ChatAIHubApplication.java       # 启动类
│   ├── config/                         # 配置类
│   ├── controller/                     # 接口层
│   ├── service/                        # 业务层
│   ├── mapper/                         # 数据层
│   ├── model/                          # 实体类
│   └── common/                         # 通用工具
└── src/main/resources
    ├── application.yml                 # 项目配置
    └── mapper/                         # MyBatis XML
```

---

## 🔒 安全规范

1. 敏感配置（API 密钥、数据库密码）统一存储在 `\.env` 文件，**永不提交到 Git**

2. 用户密码使用 BCrypt 加密存储，数据库不存储明文

3. 验证码 60 秒发送间隔限制，防止恶意刷接口

4. 密码错误 5 次自动锁定，防暴力破解

---

## 📝 版本说明

- SpringBoot 3\.5\.13：适配最新 Jakarta EE 规范

- Spring AI 1\.1\.4：支持主流大模型对接

- 所有依赖均适配 SpringBoot3，无兼容问题

> （注：文档部分内容可能由 AI 生成）
>
>

> （注：文档部分内容可能由 AI 生成）
