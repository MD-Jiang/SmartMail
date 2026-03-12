package com.smartmail.service;

import com.smartmail.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;




@Service
@Slf4j
public class ContentCleaner {
    
    public void cleanEmailContent(Email email) {
        String content = email.getContent();
        if (content == null) {
            email.setCleanedContent("");
            return;
        }
        
        // 检测内容类型并进行相应处理
        if (content.contains("<html") || content.contains("<HTML")) {
            // HTML内容处理
            String cleanedHtml = cleanHtml(content);
            email.setCleanedContent(cleanedHtml);
        } else {
            // 纯文本处理
            String cleanedText = cleanPlainText(content);
            email.setCleanedContent(cleanedText);
        }
        
        // 长度控制
        String cleanedContent = email.getCleanedContent();
        if (cleanedContent.length() > 2000) {
            email.setCleanedContent(truncateContent(cleanedContent));
        }
        
        log.debug("Cleaned content for email from {}: {}", email.getFromAddress(), email.getCleanedContent().length() + " characters");
    }
    
    private String cleanHtml(String html) {
        Document doc = Jsoup.parse(html);
        
        // 移除不需要的标签
        Elements elementsToRemove = doc.select("script, style, iframe, noscript");
        elementsToRemove.remove();
        
        // 移除内联事件处理器
        for (Element element : doc.getAllElements()) {
            element.removeAttr("onclick");
            element.removeAttr("onload");
            element.removeAttr("onerror");
            element.removeAttr("onmouseover");
            element.removeAttr("onmouseout");
        }
        
        // 移除样式属性
        for (Element element : doc.getAllElements()) {
            element.removeAttr("style");
        }
        
        // 移除隐藏元素
        Elements hiddenElements = doc.select("[style*=display:none], [style*=visibility:hidden]");
        hiddenElements.remove();
        
        // 提取纯文本
        String text = doc.text();
        
        // 清理签名和营销信息
        return removeSignatureAndMarketing(text);
    }
    
    private String cleanPlainText(String text) {
        // 清理签名和营销信息
        return removeSignatureAndMarketing(text);
    }
    
    private String removeSignatureAndMarketing(String text) {
        // 移除签名（连续短行）
        String[] lines = text.split("\\n");
        StringBuilder cleaned = new StringBuilder();
        int shortLineCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.length() < 10) {
                shortLineCount++;
                if (shortLineCount > 3) {
                    // 认为是签名，停止添加
                    break;
                }
            } else {
                shortLineCount = 0;
            }
            
            // 移除营销信息
            if (!line.contains("退订") && !line.contains("不再接收") && !line.contains(" unsubscribe ")) {
                cleaned.append(line).append("\n");
            }
        }
        
        return cleaned.toString().trim();
    }
    
    private String truncateContent(String content) {
        // 智能截断
        if (content.contains("总结") || content.contains("要点") || content.contains("关键")) {
            // 优先保留包含关键词的段落
            int summaryIndex = content.indexOf("总结");
            int pointsIndex = content.indexOf("要点");
            int keyIndex = content.indexOf("关键");
            
            int startIndex = Math.min(Math.min(summaryIndex, pointsIndex), keyIndex);
            if (startIndex > 0) {
                return content.substring(startIndex, Math.min(startIndex + 1500, content.length()));
            }
        }
        
        // 开头+结尾策略
        return content.substring(0, 500) + "..." + content.substring(content.length() - 300);
    }
}