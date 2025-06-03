package com.yokior.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 文档处理专用线程池
     */
    @Bean(name = "documentProcessingExecutor")
    public Executor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数 - 根据CPU核心数设置
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        
        // 最大线程数 - 核心线程数的2倍
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        // 队列容量 - 用于存储等待执行的任务
        executor.setQueueCapacity(50);
        
        // 线程名前缀
        executor.setThreadNamePrefix("document-process-");
        
        // 线程空闲时间 - 超过核心线程数的线程在空闲60秒后会被销毁
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略 - 当队列和线程池都满了时，使用调用者所在的线程来执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务执行完成再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(120);
        
        // 初始化线程池
        executor.initialize();
        
        return executor;
    }
    
    /**
     * 通用异步任务线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量
        executor.setQueueCapacity(25);
        
        // 线程名前缀
        executor.setThreadNamePrefix("async-task-");
        
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 初始化线程池
        executor.initialize();
        
        return executor;
    }
} 