package com.zyb.backend.constant;

/**
 * 文件常量
 */
public interface FileConstant {

    /**
     * 临时文件目录
     */
    String TMP_DIR = "tmp/";

    /**
     * 对话记忆存储目录
     */
    String CHAT_MEMORY_DIR = TMP_DIR + "chat-memory/";

    /**
     * 音频文件存储目录
     */
    String VOICE_DIR = TMP_DIR + "voice/";

    /**
     * 声音样本目录
     */
    String VOICE_SAMPLE_DIR = VOICE_DIR + "samples/";

    /**
     * 生成的音频目录
     */
    String VOICE_GENERATED_DIR = VOICE_DIR + "generated/";

    /**
     * 允许的音频文件类型
     */
    String[] ALLOWED_AUDIO_TYPES = {"mp3", "wav", "m4a", "ogg", "flac"};

    /**
     * 最大音频文件大小 (50MB)
     */
    long MAX_AUDIO_FILE_SIZE = 50 * 1024 * 1024;

    String OSS_HOST = "";
}
