package com.campus.task.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 抢单行为记录
 */
@Data
@TableName("grab_record")
public class GrabRecord {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDate grabDate;
    /**
     * 状态：0锁单中 1已确认进行 2发布者拒绝
     *       3超时释放 4主动取消
     */
    private Integer status;
    /** 是否记违规 */
    private Integer isViolation;
    private LocalDateTime createdAt;
}
