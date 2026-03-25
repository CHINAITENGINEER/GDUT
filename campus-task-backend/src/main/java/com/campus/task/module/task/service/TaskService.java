package com.campus.task.module.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.module.task.dto.*;
import com.campus.task.module.task.vo.*;

/**
 * 任务业务接口
 */
public interface TaskService {

    /** 发布任务 */
    TaskDetailVO publish(Long publisherId, TaskPublishDTO dto);

    /** 任务列表（分页） */
    Page<TaskCardVO> list(TaskQueryDTO dto);

    /** 任务详情 */
    TaskDetailVO detail(Long taskId, Long currentUserId);

    /** 抢单，返回锁单过期时间等信息 */
    GrabVO grab(Long taskId, Long userId);

    /** 发布者确认接受抢单者，返回支付页跳转信息 */
    GrabConfirmVO confirmGrab(Long taskId, Long publisherId);

    /** 发布者拒绝抢单者 */
    void rejectGrab(Long taskId, Long publisherId);

    /** 抢单者主动取消 */
    void cancelGrab(Long taskId, Long userId);

    /** 提交交付成果 */
    void submit(Long taskId, Long acceptorId, TaskSubmitDTO dto);

    /** 验收，通过时返回结算明细 */
    VerifyVO verify(Long taskId, Long publisherId, TaskVerifyDTO dto);

    /** 发布者取消任务 */
    void cancel(Long taskId, Long publisherId);

    /** 申请管理员介入 */
    void dispute(Long taskId, Long userId, String reason);

    /** 我发布的任务 */
    Page<TaskCardVO> myPublished(Long userId, Integer page, Integer pageSize, Integer status);

    /** 我抢单的任务 */
    Page<TaskCardVO> myGrabbed(Long userId, Integer page, Integer pageSize, Integer status);

    /** 定时任务：处理超时锁单 */
    void handleExpiredGrabs();
}
