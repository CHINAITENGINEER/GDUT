package com.campus.task.module.rally.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
public class RallyActivityVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Integer type;
    private String typeName;
    private String title;
    private Integer recruitCount;
    private Integer currentCount;
    private Long startTime;
    private String remark;
    /** 0进行中 1已结束 */
    private Integer status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long organizerId;
    private String organizerNickname;
    private String organizerAvatar;

    private Long createdAt;
    private List<RallyMemberVO> members;
}
