package com.campus.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园任务接单平台 启动类
 */
@SpringBootApplication
@MapperScan("com.campus.task.module.**.mapper")
@EnableScheduling
public class CampusTaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusTaskApplication.class, args);
    }
}
