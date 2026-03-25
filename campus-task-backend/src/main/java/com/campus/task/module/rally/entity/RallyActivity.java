package com.campus.task.module.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rally_activity")
public class RallyActivity {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long organizerId;
    /** 1运动 2游戏 */
    private Integer type;
    private String title;
    /** 召集总人数（包含发起人） */
    private Integer recruitCount;
    /** 当前已加入人数 */
    private Integer currentCount;
    private LocalDateTime startTime;
    private String remark;
    /** 0进行中 1已结束 */
    private Integer status;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
