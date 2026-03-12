package com.smartmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartMailApplication.class, args);
    }
}