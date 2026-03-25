package com.campus.task.module.rally.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class RallyMemberVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String nickname;
    private String avatar;
    /** 0发起人 1参与者 */
    private Integer role;
    private Long joinedAt;
}
