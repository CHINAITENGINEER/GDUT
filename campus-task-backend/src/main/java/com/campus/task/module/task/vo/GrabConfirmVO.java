package com.campus.task.module.task.vo;

import lombok.Data;

/**
 * 发布者确认接受抢单者 返回VO
 */
@Data
public class GrabConfirmVO {
    /** 跳转支付页的前端路由路径，如 /payment/pay?taskId=xxx */
    private String payUrl;
    private String message;
}
