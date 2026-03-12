package com.smartmail.service;

import com.smartmail.model.Email;
import com.smartmail.model.UserConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class MailFetcher {
    
    public List<Email> fetchEmails(UserConfig userConfig) throws MessagingException, java.io.IOException {
        List<Email> emails = new ArrayList<>();
        
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", userConfig.getImapHost());
        props.setProperty("mail.imaps.port", String.valueOf(userConfig.getImapPort()));
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.connectiontimeout", "10000");
        props.setProperty("mail.imaps.timeout", "30000");
        
        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        
        try {
            store.connect(userConfig.getImapUsername(), userConfig.getImapPassword());
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            
            Message[] messages = inbox.getMessages();
            LocalDateTime lastFetchTime = userConfig.getLastFetchTime();
            
            for (Message message : messages) {
                if (message instanceof MimeMessage) {
                    LocalDateTime sentDate = message.getSentDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    
                    if (lastFetchTime == null || sentDate.isAfter(lastFetchTime)) {
                        Email email = new Email();
                        email.setUserConfig(userConfig);
                        email.setMessageId(message.getMessageNumber() + "");
                        email.setFromAddress(InternetAddress.toString(message.getFrom()));
                        email.setSubject(message.getSubject());
                        email.setSentDate(sentDate);
                        
                        // 提取内容
                        Object content = message.getContent();
                        if (content instanceof String) {
                            email.setContent((String) content);
                        } else if (content instanceof Multipart) {
                            email.setContent(extractMultipartContent((Multipart) content));
                        }
                        
                        emails.add(email);
                        
                        // 标记为已读
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                }
            }
            
            // 更新最后抓取时间
            userConfig.setLastFetchTime(LocalDateTime.now());
            
        } finally {
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
        
        log.info("Fetched {} new emails for user {}", emails.size(), userConfig.getUsername());
        return emails;
    }
    
    private String extractMultipartContent(Multipart multipart) throws MessagingException, java.io.IOException {
        StringBuilder content = new StringBuilder();
        
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                content.append(part.getContent().toString());
            } else if (part.isMimeType("text/html")) {
                content.append(part.getContent().toString());
            } else if (part.getContent() instanceof Multipart) {
                content.append(extractMultipartContent((Multipart) part.getContent()));
            }
        }
        
        return content.toString();
    }
}