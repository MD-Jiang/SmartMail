package com.smartmail.service;

import com.smartmail.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIAnalysisService {
    
    @Value("${smartmail.ai.api-key}")
    private String apiKey;
    
    @Value("${smartmail.ai.endpoint}")
    private String endpoint;
    
    @Value("${smartmail.ai.model}")
    private String model;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public void analyzeEmail(Email email) {
        try {
            String cleanedContent = email.getCleanedContent();
            if (cleanedContent == null || cleanedContent.isEmpty()) {
                email.setCategory("OTHER");
                email.setSummary("无内容");
                return;
            }
            
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的邮件助手，擅长分类和总结");
            messages.add(systemMessage);
            
            // 用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "邮件主题：" + email.getSubject() + "\n邮件内容：" + cleanedContent + "\n\n请对这封邮件进行分类（重要/待办/通知/其他）并生成简短摘要");
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 2048);
            requestBody.put("stream", false);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);
            
            // 解析响应
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    
                    // 解析分类和摘要
                    parseAnalysisResult(content, email);
                }
            }
            
        } catch (Exception e) {
            log.error("Error analyzing email: {}", e.getMessage());
            // 降级到本地分析
            fallbackAnalyze(email);
        }
    }
    
    public void batchAnalyze(List<Email> emails) {
        try {
            if (emails.isEmpty()) {
                return;
            }
            
            // 构建批量请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的邮件助手，擅长分类和总结");
            messages.add(systemMessage);
            
            // 用户消息
            Map<String, String> userMessage = new HashMap<>();
            StringBuilder contentBuilder = new StringBuilder();
            
            for (int i = 0; i < emails.size(); i++) {
                Email email = emails.get(i);
                contentBuilder.append("邮件").append(i + 1).append("主题：").append(email.getSubject()).append("\n");
                contentBuilder.append("邮件").append(i + 1).append("内容：").append(email.getCleanedContent()).append("\n\n");
            }
            
            contentBuilder.append("请返回JSON数组，格式：[{id:1,category:\"重要\",summary:\"摘要\"},{id:2...}]");
            userMessage.put("role", "user");
            userMessage.put("content", contentBuilder.toString());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 4096);
            requestBody.put("stream", false);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);
            
            // 解析响应
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    
                    // 解析批量分析结果
                    parseBatchAnalysisResult(content, emails);
                }
            }
            
        } catch (Exception e) {
            log.error("Error batch analyzing emails: {}", e.getMessage());
            // 降级到本地分析
            for (Email email : emails) {
                fallbackAnalyze(email);
            }
        }
    }
    
    protected void fallbackAnalyze(Email email) {
        // 本地降级分析
        String content = email.getCleanedContent();
        String subject = email.getSubject();
        
        // 简单的关键词规则
        if (subject.contains("重要") || subject.contains("紧急") || content.contains("重要") || content.contains("紧急")) {
            email.setCategory("IMPORTANT");
        } else if (subject.contains("请") || subject.contains("要求") || subject.contains("需要") || content.contains("请") || content.contains("要求") || content.contains("需要")) {
            email.setCategory("TODO");
        } else if (subject.contains("通知") || subject.contains("提醒") || subject.contains("公告") || content.contains("通知") || content.contains("提醒") || content.contains("公告")) {
            email.setCategory("NOTIFICATION");
        } else {
            email.setCategory("OTHER");
        }
        
        // 生成简单摘要
        if (content.length() > 100) {
            email.setSummary(content.substring(0, 100) + "...");
        } else {
            email.setSummary(content);
        }
    }
    
    private void parseAnalysisResult(String content, Email email) {
        // 简单解析AI返回的内容
        // 实际实现中可能需要更复杂的解析逻辑
        if (content.contains("重要")) {
            email.setCategory("IMPORTANT");
        } else if (content.contains("待办")) {
            email.setCategory("TODO");
        } else if (content.contains("通知")) {
            email.setCategory("NOTIFICATION");
        } else {
            email.setCategory("OTHER");
        }
        
        // 提取摘要
        int summaryStart = content.indexOf("摘要");
        if (summaryStart > 0) {
            email.setSummary(content.substring(summaryStart + 2).trim());
        } else {
            email.setSummary(content);
        }
    }
    
    private void parseBatchAnalysisResult(String content, List<Email> emails) {
        // 简单解析JSON格式的批量结果
        // 实际实现中可能需要使用Jackson等库进行解析
        log.debug("Batch analysis result: {}", content);
        
        // 这里简化处理，实际应该解析JSON并对应到每个邮件
        for (Email email : emails) {
            fallbackAnalyze(email);
        }
    }
}