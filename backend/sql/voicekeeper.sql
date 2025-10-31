
-- 创建数据库
CREATE DATABASE IF NOT EXISTS voiceKeeper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE voiceKeeper;

-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    userNo VARCHAR(20) NOT NULL COMMENT '用户编号',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    realName VARCHAR(50) COMMENT '真实姓名',
    
    -- 联系方式
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    
    -- 个人信息
    avatar VARCHAR(500) COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    birthday DATE COMMENT '生日',
    
    -- 角色与状态
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色：user-普通用户，admin-管理员',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    
    -- 时间戳
    registerTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    lastLoginTime DATETIME COMMENT '最后登录时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete TINYINT DEFAULT 0 COMMENT '是否删除',
    
    UNIQUE KEY uk_user_no (userNo),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email),
    INDEX idx_status (status),
    INDEX idx_create_time (createTime)
) COMMENT '用户表' COLLATE = utf8mb4_unicode_ci;

-- ==================== 声音模型表 ====================
CREATE TABLE IF NOT EXISTS voice_model (
    id BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    userId BIGINT NOT NULL COMMENT '用户ID',
    modelName VARCHAR(100) NOT NULL COMMENT '模型名称(如:妈妈的声音)',
    voiceDesc VARCHAR(500) COMMENT '声音描述',
    
    -- 音频信息
    sampleAudioUrl VARCHAR(500) NOT NULL COMMENT '样本音频OSS地址',
    sampleDuration INT COMMENT '样本时长(秒)',
    sampleFileSize BIGINT COMMENT '样本文件大小(字节)',
    
    -- AI模型信息
    aiModelId VARCHAR(200) COMMENT '第三方AI模型ID(ElevenLabs/阿里云)',
    aiProvider VARCHAR(50) DEFAULT 'elevenlabs' COMMENT 'AI服务提供商',
    trainingStatus TINYINT DEFAULT 0 COMMENT '训练状态: 0-待训练 1-训练中 2-成功 3-失败',
    trainingMessage VARCHAR(500) COMMENT '训练消息(成功/失败原因)',
    
    -- 音质评估
    qualityScore DECIMAL(3,2) COMMENT '音质评分(0-5)',
    voiceType VARCHAR(20) COMMENT '声音类型: male/female/child',
    
    -- 使用统计
    useCount INT DEFAULT 0 COMMENT '使用次数',
    
    -- 元数据
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete TINYINT DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_user_id (userId),
    INDEX idx_training_status (trainingStatus),
    INDEX idx_create_time (createTime)
) COMMENT '声音模型表' COLLATE = utf8mb4_unicode_ci;

-- ==================== 声音卡片表 ====================
CREATE TABLE IF NOT EXISTS voice_card (
    id BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    userId BIGINT NOT NULL COMMENT '用户ID',
    voiceModelId BIGINT NOT NULL COMMENT '声音模型ID',
    
    -- 卡片内容
    cardTitle VARCHAR(100) COMMENT '卡片标题',
    textContent TEXT NOT NULL COMMENT '文字内容',
    aiGenerated TINYINT DEFAULT 0 COMMENT '是否AI生成: 0-否 1-是',
    
    -- 音频信息
    audioUrl VARCHAR(500) COMMENT '生成的音频OSS地址',
    audioDuration INT COMMENT '音频时长(秒)',
    audioFileSize BIGINT COMMENT '音频文件大小(字节)',
    
    -- 场景标签
    sceneTag VARCHAR(50) COMMENT '场景: morning/night/encourage/miss/custom',
    emotionTag VARCHAR(20) COMMENT '情感标签: warm/gentle/energetic/sad',
    
    -- 使用统计
    playCount INT DEFAULT 0 COMMENT '播放次数',
    shareCount INT DEFAULT 0 COMMENT '分享次数',
    lastPlayTime DATETIME COMMENT '最后播放时间',
    
    -- 元数据
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete TINYINT DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_user_id (userId),
    INDEX idx_model_id (voiceModelId),
    INDEX idx_scene_tag (sceneTag),
    INDEX idx_create_time (createTime)
) COMMENT '声音卡片表' COLLATE = utf8mb4_unicode_ci;

-- ==================== 插入测试数据 ====================

-- 插入测试用户
INSERT INTO user (id, userNo, username, password, realName, phone, email, role, status)
VALUES 
(1, 'U000001', 'testuser', '$2a$10$test.hash.password', '测试用户', '13800138000', 'test@example.com', 'user', 1),
(2, 'U000002', 'demo', '$2a$10$demo.hash.password', '演示用户', '13800138001', 'demo@example.com', 'user', 1)
ON DUPLICATE KEY UPDATE id=id;

-- 插入示例声音模型
INSERT INTO voice_model (userId, modelName, voiceDesc, sampleAudioUrl, sampleDuration, trainingStatus, aiProvider)
VALUES 
(1, '妈妈的声音', '温暖慈爱的声音', 'https://oss.example.com/samples/mom_voice.mp3', 180, 2, 'elevenlabs'),
(1, '爸爸的声音', '沉稳有力的声音', 'https://oss.example.com/samples/dad_voice.mp3', 200, 2, 'elevenlabs')
ON DUPLICATE KEY UPDATE id=id;

-- 插入示例声音卡片
INSERT INTO voice_card (userId, voiceModelId, cardTitle, textContent, sceneTag, emotionTag, aiGenerated, audioUrl)
VALUES 
(1, 1, '晚安问候', '宝贝晚安，无论多晚都要好好休息，妈妈永远爱你', 'night', 'warm', 1, 'https://oss.example.com/cards/goodnight.mp3'),
(1, 1, '早安鼓励', '早上好宝贝！新的一天开始了，加油！妈妈相信你', 'morning', 'energetic', 1, 'https://oss.example.com/cards/morning.mp3'),
(1, 2, '工作鼓励', '儿子，工作再累也要注意身体，爸爸支持你', 'encourage', 'gentle', 1, 'https://oss.example.com/cards/work_encourage.mp3')
ON DUPLICATE KEY UPDATE id=id;

