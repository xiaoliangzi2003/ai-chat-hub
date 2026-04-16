db = db.getSiblingDB('aichat');

db.createCollection("ai_chat_session", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "sessionTitle", "createTime", "updateTime", "isDelete"],
            properties: {
                _id: {bsonType: "objectId"},
                userId: {bsonType: "long", description: "用户ID，必须是数字"},
                sessionTitle: {bsonType: "string", description: "会话标题"},
                createTime: {bsonType: "date", description: "创建时间"},
                updateTime: {bsonType: "date", description: "更新时间"},
                isDelete: {bsonType: "bool", description: "软删除标记"}
            }
        }
    },
    validationLevel: "strict",
    validationAction: "error"
});

db.ai_chat_session.createIndex({userId: 1});
db.ai_chat_session.createIndex({updateTime: -1});


db.createCollection("ai_chat_message", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["sessionId", "userId", "role", "content", "createTime"],
            properties: {
                _id: {bsonType: "objectId"},
                sessionId: {bsonType: "string", description: "关联会话ID"},
                userId: {bsonType: "long", description: "用户ID"},
                role: {
                    enum: ["system", "user", "assistant", "tool"],
                    description: "消息角色：只能是 system/user/assistant/tool"
                },
                content: {bsonType: "string", description: "消息内容"},
                toolCalls: {bsonType: ["array", "null"], description: "工具调用列表"},
                toolCallId: {bsonType: ["string", "null"], description: "工具ID"},
                tokens: {bsonType: ["int", "null"], description: "token消耗"},
                createTime: {bsonType: "date", description: "消息时间"}
            }
        }
    },
    validationLevel: "strict",
    validationAction: "error"
});


db.ai_chat_message.createIndex({sessionId: 1});
db.ai_chat_message.createIndex({userId: 1});
db.ai_chat_message.createIndex({createTime: 1});

print("✅ AI 对话 MongoDB 集合创建完成：ai_chat_session、ai_chat_message");