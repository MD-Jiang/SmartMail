package com.smartmail.service;

import com.smartmail.model.*;
import com.smartmail.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ScheduledTasks {
    
    @Autowired
    private UserConfigRepository userConfigRepository;
    
    @Autowired
    private EmailRepository emailRepository;
    
    @Autowired
    private ProcessLogRepository processLogRepository;
    
    @Autowired
    private ProcessStatusRepository processStatusRepository;
    
    @Autowired
    private MailFetcher mailFetcher;
    
    @Autowired
    private ContentCleaner contentCleaner;
    
    @Autowired
    private AIAnalysisService aiAnalysisService;
    
    @Autowired
    private ClassificationService classificationService;
    
    @Autowired
    private VoiceSynthesisService voiceSynthesisService;
    
    @Scheduled(cron = "${smartmail.schedule.morning}")
    public void processEmailsMorning() {
        processEmails();
    }
    
    @Scheduled(cron = "${smartmail.schedule.noon}")
    public void processEmailsNoon() {
        processEmails();
    }
    
    public void processEmails() {
        log.info("Starting email processing task at {}", LocalDateTime.now());
        
        // 获取所有用户配置
        List<UserConfig> userConfigs = userConfigRepository.findAll();
        
        for (UserConfig userConfig : userConfigs) {
            ProcessLog processLog = new ProcessLog();
            processLog.setUserConfig(userConfig);
            processLog.setStartTime(LocalDateTime.now());
            processLog.setStatus("STARTED");
            
            try {
                // 检查并获取锁
                ProcessStatus processStatus = processStatusRepository.findByUserConfigId(userConfig.getId())
                        .orElse(new ProcessStatus());
                
                if (processStatus.getLockOwner() != null && 
                    processStatus.getLockTime().plusMinutes(30).isAfter(LocalDateTime.now())) {
                    log.warn("Task already running for user {}, skipping", userConfig.getUsername());
                    continue;
                }
                
                // 获取锁
                processStatus.setUserConfig(userConfig);
                processStatus.setCurrentState("INIT");
                processStatus.setLockOwner(Thread.currentThread().getName());
                processStatus.setLockTime(LocalDateTime.now());
                processStatusRepository.save(processStatus);
                
                // 1. 抓取邮件
                processStatus.setCurrentState("FETCHING");
                processStatusRepository.save(processStatus);
                List<Email> emails = mailFetcher.fetchEmails(userConfig);
                userConfigRepository.save(userConfig);
                
                // 保存新邮件
                emailRepository.saveAll(emails);
                processLog.setProcessedCount(emails.size());
                
                // 2. 清洗内容
                processStatus.setCurrentState("CLEANING");
                processStatusRepository.save(processStatus);
                for (Email email : emails) {
                    contentCleaner.cleanEmailContent(email);
                }
                emailRepository.saveAll(emails);
                
                // 3. AI分析
                processStatus.setCurrentState("ANALYZING");
                processStatusRepository.save(processStatus);
                aiAnalysisService.batchAnalyze(emails);
                emailRepository.saveAll(emails);
                
                // 4. 分类决策
                processStatus.setCurrentState("CLASSIFYING");
                processStatusRepository.save(processStatus);
                for (Email email : emails) {
                    classificationService.classifyEmail(email);
                }
                emailRepository.saveAll(emails);
                
                // 5. 会话聚合
                List<Email> aggregatedEmails = classificationService.aggregateConversations(emails);
                
                // 6. 语音合成
                processStatus.setCurrentState("SYNTHESIZING");
                processStatusRepository.save(processStatus);
                voiceSynthesisService.synthesizeEmails(aggregatedEmails);
                
                // 7. 标记为已处理
                for (Email email : emails) {
                    email.setProcessed(true);
                }
                emailRepository.saveAll(emails);
                
                processStatus.setCurrentState("COMPLETED");
                processLog.setStatus("SUCCESS");
                processLog.setEndTime(LocalDateTime.now());
                
            } catch (MessagingException | java.io.IOException e) {
                log.error("Error fetching emails: {}", e.getMessage());
                processLog.setStatus("FAILED");
                processLog.setErrorMessage(e.getMessage());
            } catch (Exception e) {
                log.error("Error processing emails: {}", e.getMessage());
                processLog.setStatus("FAILED");
                processLog.setErrorMessage(e.getMessage());
            } finally {
                // 释放锁
                ProcessStatus processStatus = processStatusRepository.findByUserConfigId(userConfig.getId()).orElse(null);
                if (processStatus != null) {
                    processStatus.setLockOwner(null);
                    processStatus.setLockTime(null);
                    processStatusRepository.save(processStatus);
                }
                
                // 保存处理日志
                processLogRepository.save(processLog);
            }
        }
        
        // 清理旧音频文件
        voiceSynthesisService.cleanupOldAudioFiles(7); // 保留7天
        
        log.info("Email processing task completed at {}", LocalDateTime.now());
    }
}