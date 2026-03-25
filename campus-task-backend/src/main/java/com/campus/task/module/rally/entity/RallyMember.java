package com.campus.task.module.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rally_member")
public class RallyMember {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long rallyId;
    private Long userId;
    /** 0发起人 1参与者 */
    private Integer role;
    /** 1在队 0已退出 */
    private Integer status;
    private LocalDateTime joinedAt;
    private LocalDateTime quitAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
