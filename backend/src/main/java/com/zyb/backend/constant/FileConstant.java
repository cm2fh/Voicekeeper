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
     * 允许的音频文件类型
     */
    String[] ALLOWED_AUDIO_TYPES = {"mp3", "wav", "m4a", "ogg", "flac"};
}
