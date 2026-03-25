package com.campus.task.scheduler;

import com.campus.task.module.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：处理超时锁单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrabExpireScheduler {

    private final TaskService taskService;

    /**
     * 每分钟扫描一次超时锁单并自动释放
     */
    @Scheduled(fixedDelay = 60_000)
    public void handleExpiredGrabs() {
        log.debug("[定时任务] 开始扫描超时锁单...");
        taskService.handleExpiredGrabs();
    }
}
