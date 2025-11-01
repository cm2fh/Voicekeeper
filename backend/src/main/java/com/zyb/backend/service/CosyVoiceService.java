package com.zyb.backend.service;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam.SpeechSynthesisParamBuilder;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.audio.ttsv2.enrollment.Voice;
import com.alibaba.dashscope.audio.ttsv2.enrollment.VoiceEnrollmentService;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Service
@Slf4j
public class CosyVoiceService {

    @Value("${voiceKeeper.voice.cosy-voice.api-key}")
    private String apiKey;

    @Value("${voiceKeeper.voice.cosy-voice.model:cosyvoice-v3}")
    private String model;

    /**
     * 创建音色（声音克隆）
     */
    public String createVoice(String audioUrl) throws NoApiKeyException, InputRequiredException {
        try {
            VoiceEnrollmentService service = new VoiceEnrollmentService(apiKey);
            Voice voice = service.createVoice(model, "voice", audioUrl);
            
            return voice.getVoiceId();
        } catch (Exception e) {
            log.error("创建音色失败", e);
            return "创建音色失败，请重试";
        }
    }

    /**
     * 查询音色状态
     */
    public String queryVoiceStatus(String voiceId) throws Exception {
        VoiceEnrollmentService service = new VoiceEnrollmentService(apiKey);
        Voice result = service.queryVoice(voiceId);
        String status = result.getStatus();
        log.info("查询音色状态: voiceId={}, status={}", voiceId, status);

        return status;
    }

    /**
     * 删除音色
     */
    public void deleteVoice(String voiceId) throws Exception {
        VoiceEnrollmentService service = new VoiceEnrollmentService(apiKey);
        service.deleteVoice(voiceId);
        log.info("音色已删除: voiceId={}", voiceId);
    }

    /**
     * 语音合成（同步调用）
     */
    public byte[] synthesizeSpeech(String voiceId, String text) throws Exception {
        return synthesizeSpeech(voiceId, text, null, null, null);
    }

    /**
     * 语音合成（同步调用，带情感控制）
     * 
     * @param voiceId 音色ID
     * @param text 文本内容
     * @param emotion 情感指令（可选，如"温柔的"、"活泼的"）
     * @param speechRate 语速（可选，0.5-2.0，默认1.0）
     * @param pitchRate 音调（可选，0.5-2.0，默认1.0）
     */
    public byte[] synthesizeSpeech(String voiceId, String text, String emotion, Float speechRate, Float pitchRate) throws Exception {
        log.info("开始语音合成: voiceId={}, 文字长度={}, 情感={}", voiceId, text.length(), emotion);

        // 1. 构建合成参数
        SpeechSynthesisParamBuilder<?, ?> builder = SpeechSynthesisParam.builder()
                .model(model)
                .voice(voiceId)
                .apiKey(apiKey)
                .format(SpeechSynthesisAudioFormat.MP3_44100HZ_MONO_256KBPS);

        // 设置语速（默认1.0）
        if (speechRate != null) {
            builder.speechRate(speechRate);
        }

        // 设置音调（默认1.0）
        if (pitchRate != null) {
            builder.pitchRate(pitchRate);
        }

        // 设置情感指令
        if (emotion != null && !emotion.trim().isEmpty()) {
            builder.instruction("你说话的情感是" + emotion + "。");
        }

        SpeechSynthesisParam param = builder.build();

        // 2. 创建合成器（传入参数）
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);

        // 3. 调用合成（传入文字文本）
        ByteBuffer audioBuffer = synthesizer.call(text);

        log.info("语音合成成功: requestId={}, 大小={}KB",
                synthesizer.getLastRequestId(),
                audioBuffer.remaining() / 1024);

        // 4. 转换为字节数组
        byte[] audioData = new byte[audioBuffer.remaining()];
        audioBuffer.get(audioData);

        return audioData;
    }
}


