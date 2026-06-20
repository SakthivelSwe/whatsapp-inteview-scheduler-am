package com.dummby.interviewscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableCaching
public class InterviewSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewSchedulerApplication.class, args);
    }
}

