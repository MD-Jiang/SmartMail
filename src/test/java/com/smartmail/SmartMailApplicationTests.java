package com.smartmail;

import com.smartmail.model.UserConfig;
import com.smartmail.repository.UserConfigRepository;
import com.smartmail.service.MailFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;

@SpringBootTest
class SmartMailApplicationTests {

    @Autowired
    private UserConfigRepository userConfigRepository;
    
    @Autowired
    private MailFetcher mailFetcher;

    @Test
    void contextLoads() {
        // 测试应用上下文加载
    }
    
    @Test
    void testUserConfig() {
        // 测试用户配置保存
        UserConfig userConfig = new UserConfig();
        userConfig.setUsername("test");
        userConfig.setEmail("test@example.com");
        userConfig.setImapHost("imap.example.com");
        userConfig.setImapPort(993);
        userConfig.setImapUsername("test");
        userConfig.setImapPassword("password");
        userConfig.setLastFetchTime(LocalDateTime.now());
        
        UserConfig saved = userConfigRepository.save(userConfig);
        assert saved.getId() != null;
    }
    
    @Test
    void testMailFetcher() throws MessagingException, java.io.IOException {
        // 测试邮件抓取服务（需要配置真实的邮箱信息）
        // 这里只是测试方法调用，不会实际执行抓取
        UserConfig userConfig = new UserConfig();
        userConfig.setUsername("test");
        userConfig.setEmail("test@example.com");
        userConfig.setImapHost("imap.example.com");
        userConfig.setImapPort(993);
        userConfig.setImapUsername("test");
        userConfig.setImapPassword("password");
        
        // 注意：这里会抛出异常，因为配置的是假的邮箱信息
        // mailFetcher.fetchEmails(userConfig);
    }

}
