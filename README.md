# VoiceKeeper

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot) [![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue.svg)](https://spring.io/projects/spring-ai) [![Vue.js](https://img.shields.io/badge/Vue.js-3.5.17-4FC08D.svg)](https://vuejs.org/) [![TypeScript](https://img.shields.io/badge/TypeScript-5.8.0-3178C6.svg)](https://www.typescriptlang.org/)

---

## 📖 项目简介

VoiceKeeper 是一个基于 **ReAct Agent 模式** 的 AI 原生应用，通过声音克隆和情感卡片技术，让用户能够保存和传递最珍贵的声音记忆。

### 🌟 核心亮点

- **AI Agent 驱动**：采用 ReAct 模式，自然语言对话即可完成所有操作
- **声音克隆**：基于阿里云 CosyVoice，10-20 秒音频即可克隆任意声音
- **对话式交互**：无需填表单，直接对话 "用妈妈的声音给我晚安"，AI 自动完成全流程
- **语义检索**：Elasticsearch 向量检索，精准匹配用户需求
- **实时响应**：SSE 流式输出，展示 AI 思考过程
- **场景化设计**：支持早安问候、晚安陪伴、鼓励打气、思念表达等多种情感场景

---

## 🎯 应用场景

| 场景 | 描述 | 用户群体 |
|------|------|---------|
| 💑 **异地恋人** | 用对方的声音说晚安，即使相隔千里也能感受温暖 | 异地情侣 |
| 👨‍👩‍👧‍👦 **留学生家庭** | 保存父母的声音，在国外也能听到家的问候 | 留学生、父母 |
| 👴 **老人关怀** | 记录长辈的声音，成为永恒的回忆 | 子女、孙辈 |
| 🚗 **车载伴侣** | 开车时听到家人的鼓励，缓解疲劳保持专注 | 司机、通勤族 |
| 🎁 **特别礼物** | 定制声音卡片，送给最在乎的人 | 所有用户 |

---

## 🚀 快速开始

### 环境要求

#### 后端

- **JDK**: 21+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Elasticsearch**: 8.0+

#### 前端

- **Node.js**: 18+
- **npm**: 9+ 或 **pnpm**: 8+

### 1️⃣ 克隆项目

```bash
git clone https://github.com/yourusername/VoiceKeeper.git
cd VoiceKeeper
```

### 2️⃣ 数据库初始化

```bash
# 导入数据库
mysql -u root -p < backend/sql/voicekeeper.sql
```

### 3️⃣ 配置后端

创建 `backend/src/main/resources/application-secret.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/voiceKeeper?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

  ai:
    dashscope:
      api-key: sk-your-dashscope-api-key  # API Key

# OSS
aliyun:
  oss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: your-access-key
    access-key-secret: your-access-secret
    bucket-name: voice-keeper
    url-prefix: https://voice-keeper.oss-cn-beijing.aliyuncs.com/

# CosyVoice
cosy-voice:
  api-key: your-cosyvoice-api-key
  base-url: https://your-cosyvoice-endpoint
```

### 4️⃣ 启动后端

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

后端将在 `http://localhost:8101` 启动

### 5️⃣ 配置前端

创建 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8101/api
```

### 6️⃣ 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动

### 7️⃣ 访问应用

打开浏览器访问 `http://localhost:5173`，注册账号后即可开始使用！

---

## 🏗️ 技术架构

### 系统架构图

```
        ┌─────────────────────────────────────────────────────────────┐
        │                         用户界面                              │
        │  Vue 3.5 + TypeScript + Element Plus + Pinia                │
        └─────────────────┬───────────────────────────────────────────┘
                          │ SSE 流式通信
                          │ RESTful API
        ┌─────────────────▼───────────────────────────────────────────┐
        │                     Spring Boot 后端                        │
        │  ┌──────────────────────────────────────────────────────┐  │
        │  │              VoiceKeeper AI Agent                     │ │
        │  │         (ReAct: Reasoning + Acting)                   │ │
        │  │                                                       │ │
        │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │ │
        │  │  │  Think   │→ │   Act    │→ │ Reflect  │             │ │
        │  │  │ 意图识别  │   │ 工具调用  │   │ 结果反思  │            │ │
        │  │  └──────────┘  └──────────┘  └──────────┘             │ │
        │  └────────────────────┬─────────────────────────────────┘  │
        │                       │                                    │
        │  ┌────────────────────▼─────────────────────────────────┐  │
        │  │              工具系统 (Tool System)                   │  │
        │  │  ┌──────────────┐  ┌──────────────┐  ┌───────────┐   │  │
        │  │  │  声音模型检索  │  │   声音克隆     │  │  语音合成  │   │  │
        │  │  │searchVoiceModel    cloneVoice  │  │synthesize │   │  │
        │  │  └──────────────┘  └──────────────┘  └───────────┘   │  │
        │  │  ┌──────────────┐  ┌──────────────┐  ┌───────────┐   │  │
        │  │  │卡片创建       │  │语义搜索       │  │卡片检索     │   │  │
        │  │  │createCard    │  │semanticSearch│  │searchCard │   │  │
        │  │  └──────────────┘  └──────────────┘  └───────────┘   │  │
        │  └──────────────────────────────────────────────────────┘  │
        └────────┬───────────────┬───────────────┬───────────────────┘
                 │               │               │
        ┌────────▼──────┐ ┌──────▼─────┐  ┌──────▼─────────┐
        │     MySQL     │ │ Redis/File │  │ Elasticsearch  │
        │   (业务数据)   │ │  (会话记忆)  │  │   (向量检索)    │
        └───────────────┘ └────────────┘  └────────────────┘
                 │
        ┌────────▼────────────────────────────────────────────────┐
        │                    第三方 AI 服务                         │
        │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐    │
        │  │   通义千问     │  │  CosyVoice  │  │  阿里云 OSS  │    │
        │  │  (对话理解)    │  │  (声音克隆)   │  │  (文件存储)  │    │
        │  └──────────────┘  └──────────────┘  └─────────────┘    │
        └─────────────────────────────────────────────────────────┘
```

### 核心技术亮点

#### 1. ReAct Agent 模式

```java
@Override
public String step() {
    setFinalAnswer(null);
    // Think: 分析用户意图，决定下一步行动
    log.info("思考中...");
    boolean shouldAct = think();
    if (shouldAct) {
        // Act: 执行工具调用
        log.info("行动中...");
        return act();
    }
    // 返回最终答案
    String answer = getFinalAnswer();
    if (answer != null && !answer.isEmpty()) {
        setState(AgentState.FINISHED);
        return answer;
    }
    setState(AgentState.FINISHED);
    return "思考完成 - 无需行动";
}
```

**优势：**

- 自主决策：AI 自行判断是否需要调用工具
- 多步推理：可以连续执行多个工具调用
- 自我反思：执行后评估结果，决定是否继续

#### 2. 工具系统 (Tool System)

系统提供 7 个核心工具，AI 根据对话内容自主选择调用：

```java
@Tool(description = """
    【声音克隆】克隆用户上传的声音样本
    使用时机：检测到用户上传了音频文件
    参数：userId, audioUrl, modelName
    返回：克隆任务信息，1-2分钟后可用
    """)
public String cloneVoice(
    @ToolParam(description = "用户ID") Long userId,
    @ToolParam(description = "音频OSS地址") String audioUrl,
    @ToolParam(description = "声音模型名称") String modelName
) {
    // 实现代码...
}
```

**可用工具：**

- `searchVoiceModelByName`: 查询声音模型
- `searchUserCards`: 查询用户的语音卡片
- `cloneVoice`: 克隆声音
- `synthesizeVoice`: 语音合成
- `createVoiceCard`: 创建语音卡片
- `searchCardsBySemantic`: 语义搜索卡片
- `doTerminate`: 终止执行

#### 3. 向量检索系统

基于 Elasticsearch 实现语义搜索，而非简单的关键词匹配：

```java
@Service
public class VoiceCardVectorService {
    
    @Resource
    private ElasticsearchVectorStore vectorStore;
    
    public List<VoiceCard> semanticSearch(Long userId, String query, 
                                          String sceneFilter, int topK) {
        // 1. 向量化查询
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(0.5);
        
        // 2. 执行搜索
        List<Document> results = vectorStore.similaritySearch(request);
        
        // 3. 解析结果
        return results.stream()
                .map(doc -> parseDocument(doc))
                .filter(card -> card.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
}
```

**优势：**

- 理解语义：理解 "温暖的问候" = "早安卡片"
- 模糊匹配：即使关键词不完全一致也能匹配
- 智能排序：按相似度排序，返回最相关结果

#### 4. SSE 流式通信

实时展示 AI 的思考和执行过程：

```typescript
// 前端接收 SSE
const eventSource = new EventSource(
  `/api/ai/chat?message=${encodeURIComponent(message)}`
)

eventSource.addEventListener('step', (event) => {
  const step = JSON.parse(event.data)
  console.log(`Step ${step.number}: ${step.text}`)
  // 展示 AI 思考过程
})

eventSource.addEventListener('complete', (event) => {
  const result = JSON.parse(event.data)
  console.log('AI 回复:', result.content)
  // 展示最终答案
})
```

---

## 🎨 功能展示

### 1. 对话式交互

```
用户：用妈妈的声音给我晚安

AI：思考中...
✓ Step 1: 正在查找妈妈的声音模型
✓ Step 2: 已找到模型 ID: 5
✓ Step 3: 正在生成晚安文案（50-70字）
✓ Step 4: 正在合成语音...
✓ Step 5: 正在创建卡片...

AI：卡片创建成功！
【妈妈的晚安问候】
音频: https://voice-keeper.oss.../voice_123.mp3
卡片 ID: 8
```

### 2. 声音克隆

```
用户：[拖拽上传 mom_voice.mp3]

AI：检测到音频文件，请问这是谁的声音？

用户：这是我妈妈的声音

AI：好的，正在克隆妈妈的声音，预计需要 1-2 分钟...
（自动轮询检测，完成后通知）

AI：妈妈的声音已克隆成功！现在可以使用这个声音创建卡片了。
```

### 3. 智能检索

```
用户：播放适合睡前听的卡片

AI：为您找到 3 张适合睡前听的卡片：

1. 【妈妈的晚安问候】- 播放 12 次
2. 【爸爸的晚安祝福】- 播放 8 次
3. 【奶奶的睡前故事】- 播放 5 次

请问要播放哪一张？
```

---

## 📊 数据库设计

### 核心表结构

#### user - 用户表

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(500),
    role VARCHAR(20) DEFAULT 'user',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### voice_model - 声音模型表

```sql
CREATE TABLE voice_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    userId BIGINT NOT NULL,
    modelName VARCHAR(100) NOT NULL,
    sampleAudioUrl VARCHAR(500),      -- 样本音频地址
    aiModelId VARCHAR(200),           -- CosyVoice 模型 ID
    trainingStatus TINYINT,           -- 0-待训练 1-训练中 2-成功 3-失败
    useCount INT DEFAULT 0,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### voice_card - 语音卡片表

```sql
CREATE TABLE voice_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    userId BIGINT NOT NULL,
    voiceModelId BIGINT NOT NULL,
    cardTitle VARCHAR(100),           -- 卡片标题
    textContent TEXT NOT NULL,        -- 文字内容
    audioUrl VARCHAR(500),            -- 音频地址
    sceneTag VARCHAR(50),             -- morning/night/encourage/miss
    playCount INT DEFAULT 0,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔧 API 文档

启动后访问 Knife4j API 文档：`http://localhost:8101/api/doc.html`

### 核心接口

#### AI 对话接口 (SSE)

```
GET /api/ai/chat
参数:
  - message: 用户消息
  - conversationId: 会话ID（可选）
响应: text/event-stream
```

#### 上传音频

```
POST /api/file/upload
Content-Type: multipart/form-data
参数:
  - file: 音频文件
  - fileType: voice_sample
响应: { "url": "https://oss.../audio.mp3" }
```

#### 获取卡片列表

```
GET /api/voice-card/list
参数:
  - sceneTag: 场景标签（可选）
  - sortField: playCount (按播放次数排序)
  - sortOrder: descend
响应: { "records": [...], "total": 10 }
```

---

## 🎯 开发计划

- [ ] 完善文案生成
- [ ] 优化 AI 推理准确性
- [ ] 增加更多场景模板

---

## 🙏 致谢

感谢以下开源项目和服务：

- [Spring AI](https://spring.io/projects/spring-ai) - 企业级 AI 集成框架
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Element Plus](https://element-plus.org/) - Vue 3 组件库
- [通义千问](https://dashscope.aliyun.com/) - 阿里云大语言模型
- [CosyVoice](https://www.aliyun.com/product/ai/cosyvoice) - 阿里云语音克隆服务
- [Elasticsearch](https://www.elastic.co/) - 分布式搜索引擎
