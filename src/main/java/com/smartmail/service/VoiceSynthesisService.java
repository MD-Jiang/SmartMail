package com.smartmail.service;

import com.smartmail.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class VoiceSynthesisService {
    
    @Value("${smartmail.voice.rate}")
    private int rate;
    
    @Value("${smartmail.voice.pitch}")
    private float pitch;
    
    @Value("${smartmail.voice.volume}")
    private float volume;
    
    @Value("${smartmail.storage.audio-path}")
    private String audioPath;
    
    private boolean ttsAvailable = false;
    
    public VoiceSynthesisService() {
        // 尝试初始化FreeTTS引擎
        try {
            Class.forName("javax.speech.Central");
            Class.forName("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            // 延迟初始化，避免启动时出错
            ttsAvailable = true;
        } catch (ClassNotFoundException e) {
            log.warn("FreeTTS not available, voice synthesis will be disabled: {}", e.getMessage());
            ttsAvailable = false;
        }
    }
    
    public void synthesizeEmails(List<Email> emails) {
        if (emails.isEmpty()) {
            return;
        }
        
        if (!ttsAvailable) {
            log.warn("Voice synthesis is disabled, skipping");
            return;
        }
        
        try {
            // 使用反射创建合成器，避免编译时依赖
            Class<?> centralClass = Class.forName("javax.speech.Central");
            Class<?> synthesizerModeDescClass = Class.forName("javax.speech.synthesis.SynthesizerModeDesc");
            Object synthesizerModeDesc = synthesizerModeDescClass.getConstructor(String.class).newInstance("general");
            Object synthesizer = centralClass.getMethod("createSynthesizer", synthesizerModeDescClass).invoke(null, synthesizerModeDesc);
            
            // 调用方法
            synthesizer.getClass().getMethod("allocate").invoke(synthesizer);
            synthesizer.getClass().getMethod("resume").invoke(synthesizer);
            
            // 设置语音参数
            Object synthProps = synthesizer.getClass().getMethod("getSynthesizerProperties").invoke(synthesizer);
            synthProps.getClass().getMethod("setSpeakingRate", int.class).invoke(synthProps, rate);
            synthProps.getClass().getMethod("setPitch", float.class).invoke(synthProps, pitch);
            synthProps.getClass().getMethod("setVolume", float.class).invoke(synthProps, volume);
            
            // 生成总述
            int importantCount = (int) emails.stream().filter(e -> "IMPORTANT".equals(e.getCategory())).count();
            int todoCount = (int) emails.stream().filter(e -> "TODO".equals(e.getCategory())).count();
            int notificationCount = (int) emails.stream().filter(e -> "NOTIFICATION".equals(e.getCategory())).count();
            
            String summary = String.format("您有%d封重要邮件，%d封待办邮件，%d封通知邮件。", importantCount, todoCount, notificationCount);
            log.info("Synthesizing summary: {}", summary);
            synthesizer.getClass().getMethod("speakPlainText", String.class, Object.class).invoke(synthesizer, summary, null);
            
            // 等待完成
            Class<?> synthesizerClass = Class.forName("javax.speech.synthesis.Synthesizer");
            int QUEUE_EMPTY = (int) synthesizerClass.getField("QUEUE_EMPTY").get(null);
            synthesizer.getClass().getMethod("waitEngineState", int.class).invoke(synthesizer, QUEUE_EMPTY);
            
            // 分段播报邮件
            for (int i = 0; i < emails.size(); i++) {
                Email email = emails.get(i);
                String message = String.format("第%d封，来自%s，%s。%s", 
                        i + 1, 
                        extractSenderName(email.getFromAddress()), 
                        email.getCategory(), 
                        email.getSummary());
                
                log.info("Synthesizing email {}: {}", i + 1, message);
                synthesizer.getClass().getMethod("speakPlainText", String.class, Object.class).invoke(synthesizer, message, null);
                synthesizer.getClass().getMethod("waitEngineState", int.class).invoke(synthesizer, QUEUE_EMPTY);
                
                // 分段之间插入静音间隔
                Thread.sleep(300);
            }
            
            // 释放资源
            synthesizer.getClass().getMethod("deallocate").invoke(synthesizer);
            
        } catch (Exception e) {
            log.error("Error synthesizing voice: {}", e.getMessage());
            ttsAvailable = false;
        }
    }
    
    public String synthesizeToFile(List<Email> emails) {
        if (emails.isEmpty()) {
            return null;
        }
        
        if (!ttsAvailable) {
            log.warn("Voice synthesis is disabled, skipping");
            return null;
        }
        
        // 创建音频目录
        File audioDir = new File(audioPath);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        
        // 生成文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "smartmail_" + timestamp + ".wav";
        String filePath = audioDir.getAbsolutePath() + File.separator + fileName;
        
        try {
            // 使用反射创建合成器
            Class<?> centralClass = Class.forName("javax.speech.Central");
            Class<?> synthesizerModeDescClass = Class.forName("javax.speech.synthesis.SynthesizerModeDesc");
            Object synthesizerModeDesc = synthesizerModeDescClass.getConstructor(String.class).newInstance("general");
            Object synthesizer = centralClass.getMethod("createSynthesizer", synthesizerModeDescClass).invoke(null, synthesizerModeDesc);
            
            // 调用方法
            synthesizer.getClass().getMethod("allocate").invoke(synthesizer);
            synthesizer.getClass().getMethod("resume").invoke(synthesizer);
            
            // 设置语音参数
            Object synthProps = synthesizer.getClass().getMethod("getSynthesizerProperties").invoke(synthesizer);
            synthProps.getClass().getMethod("setSpeakingRate", int.class).invoke(synthProps, rate);
            synthProps.getClass().getMethod("setPitch", float.class).invoke(synthProps, pitch);
            synthProps.getClass().getMethod("setVolume", float.class).invoke(synthProps, volume);
            
            // 生成总述
            int importantCount = (int) emails.stream().filter(e -> "IMPORTANT".equals(e.getCategory())).count();
            int todoCount = (int) emails.stream().filter(e -> "TODO".equals(e.getCategory())).count();
            int notificationCount = (int) emails.stream().filter(e -> "NOTIFICATION".equals(e.getCategory())).count();
            
            String summary = String.format("您有%d封重要邮件，%d封待办邮件，%d封通知邮件。", importantCount, todoCount, notificationCount);
            synthesizer.getClass().getMethod("speakPlainText", String.class, Object.class).invoke(synthesizer, summary, null);
            
            // 等待完成
            Class<?> synthesizerClass = Class.forName("javax.speech.synthesis.Synthesizer");
            int QUEUE_EMPTY = (int) synthesizerClass.getField("QUEUE_EMPTY").get(null);
            synthesizer.getClass().getMethod("waitEngineState", int.class).invoke(synthesizer, QUEUE_EMPTY);
            
            // 分段播报邮件
            for (int i = 0; i < emails.size(); i++) {
                Email email = emails.get(i);
                String message = String.format("第%d封，来自%s，%s。%s", 
                        i + 1, 
                        extractSenderName(email.getFromAddress()), 
                        email.getCategory(), 
                        email.getSummary());
                
                synthesizer.getClass().getMethod("speakPlainText", String.class, Object.class).invoke(synthesizer, message, null);
                synthesizer.getClass().getMethod("waitEngineState", int.class).invoke(synthesizer, QUEUE_EMPTY);
            }
            
            // 释放资源
            synthesizer.getClass().getMethod("deallocate").invoke(synthesizer);
            
            log.info("Audio file generated: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("Error synthesizing to file: {}", e.getMessage());
            ttsAvailable = false;
            return null;
        }
    }
    
    private String extractSenderName(String fromAddress) {
        // 简单提取发件人名称
        if (fromAddress.contains("<")) {
            int start = fromAddress.indexOf('"');
            int end = fromAddress.indexOf('"', start + 1);
            if (start > 0 && end > start) {
                return fromAddress.substring(start + 1, end);
            }
            start = 0;
            end = fromAddress.indexOf('<');
            if (end > start) {
                return fromAddress.substring(start, end).trim();
            }
        }
        return fromAddress;
    }
    
    public void cleanupOldAudioFiles(int daysToKeep) {
        File audioDir = new File(audioPath);
        if (!audioDir.exists()) {
            return;
        }
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000);
        
        File[] files = audioDir.listFiles((dir, name) -> name.endsWith(".wav"));
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        log.info("Deleted old audio file: {}", file.getAbsolutePath());
                    } else {
                        log.warn("Failed to delete old audio file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
    }
}