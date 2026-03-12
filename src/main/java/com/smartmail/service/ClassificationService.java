package com.smartmail.service;

import com.smartmail.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ClassificationService {
    
    public enum EmailCategory {
        IMPORTANT(1, "重要"),
        TODO(2, "待办"),
        NOTIFICATION(3, "通知"),
        OTHER(4, "其他");
        
        private final int priority;
        private final String name;
        
        EmailCategory(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getName() {
            return name;
        }
    }
    
    private final Set<String> importantSenders = new HashSet<>();
    private final Set<String> todoSenders = new HashSet<>();
    private final Set<String> notificationSenders = new HashSet<>();
    
    public void classifyEmail(Email email) {
        // 基于发件人历史行为进行分类
        String sender = email.getFromAddress();
        if (importantSenders.contains(sender)) {
            email.setCategory(EmailCategory.IMPORTANT.name());
        } else if (todoSenders.contains(sender)) {
            email.setCategory(EmailCategory.TODO.name());
        } else if (notificationSenders.contains(sender)) {
            email.setCategory(EmailCategory.NOTIFICATION.name());
        }
        
        // 计算优先级
        int priority = calculatePriority(email);
        log.debug("Email from {} classified as {} with priority {}", sender, email.getCategory(), priority);
    }
    
    public List<Email> sortEmailsByPriority(List<Email> emails) {
        emails.sort((e1, e2) -> {
            int p1 = calculatePriority(e1);
            int p2 = calculatePriority(e2);
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
            // 优先级相同时按时间排序
            return e2.getSentDate().compareTo(e1.getSentDate());
        });
        return emails;
    }
    
    public List<Email> aggregateConversations(List<Email> emails) {
        Map<String, List<Email>> conversationMap = new HashMap<>();
        
        for (Email email : emails) {
            String baseSubject = extractBaseSubject(email.getSubject());
            conversationMap.computeIfAbsent(baseSubject, k -> new ArrayList<>()).add(email);
        }
        
        List<Email> aggregatedEmails = new ArrayList<>();
        for (List<Email> conversation : conversationMap.values()) {
            // 按时间排序，取最新的一封作为代表
            conversation.sort((e1, e2) -> e2.getSentDate().compareTo(e1.getSentDate()));
            Email latestEmail = conversation.get(0);
            
            // 更新摘要，添加会话信息
            if (conversation.size() > 1) {
                latestEmail.setSummary("与" + extractSenderName(latestEmail.getFromAddress()) + "有" + conversation.size() + "封往来邮件。" + latestEmail.getSummary());
            }
            
            aggregatedEmails.add(latestEmail);
        }
        
        return aggregatedEmails;
    }
    
    private int calculatePriority(Email email) {
        int basePriority = 4; // 默认最低优先级
        String sender = email.getFromAddress();
        String category = email.getCategory();
        
        // 基于分类设置基础优先级
        if (EmailCategory.IMPORTANT.name().equals(category)) {
            basePriority = 1;
        } else if (EmailCategory.TODO.name().equals(category)) {
            basePriority = 2;
        } else if (EmailCategory.NOTIFICATION.name().equals(category)) {
            basePriority = 3;
        }
        
        // 基于发件人域调整优先级
        String domain = extractDomain(sender);
        if (domain != null) {
            if (domain.contains("company.com")) { // 假设公司域名
                basePriority = Math.max(1, basePriority - 1);
            } else if (domain.contains("gmail.com") || domain.contains("163.com")) {
                // 普通邮箱，保持原有优先级
            }
        }
        
        // 基于关键词调整优先级
        String subject = email.getSubject().toLowerCase();
        String content = email.getCleanedContent().toLowerCase();
        
        if (subject.contains("紧急") || subject.contains("重要") || content.contains("紧急") || content.contains("重要")) {
            basePriority = Math.max(1, basePriority - 1);
        }
        
        return basePriority;
    }
    
    private String extractBaseSubject(String subject) {
        // 移除Re:、Fwd:等前缀
        Pattern pattern = Pattern.compile("^(Re:|Fwd:|FW:|回复:|转发:)\s*");
        return pattern.matcher(subject).replaceFirst("").trim();
    }
    
    private String extractDomain(String emailAddress) {
        int atIndex = emailAddress.indexOf('@');
        if (atIndex > 0 && atIndex < emailAddress.length() - 1) {
            return emailAddress.substring(atIndex + 1);
        }
        return null;
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
    
    public void learnFromUserAction(Email email, String action) {
        // 学习用户行为，更新发件人分类
        String sender = email.getFromAddress();
        
        switch (action) {
            case "mark_important":
                importantSenders.add(sender);
                todoSenders.remove(sender);
                notificationSenders.remove(sender);
                break;
            case "mark_todo":
                todoSenders.add(sender);
                importantSenders.remove(sender);
                notificationSenders.remove(sender);
                break;
            case "mark_notification":
                notificationSenders.add(sender);
                importantSenders.remove(sender);
                todoSenders.remove(sender);
                break;
            case "mark_other":
                importantSenders.remove(sender);
                todoSenders.remove(sender);
                notificationSenders.remove(sender);
                break;
        }
        
        log.debug("Learned from user action: {} for sender {}", action, sender);
    }
}