package com.campus.task.module.review.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 评价条目VO（返回给前端）
 */
@Data
public class ReviewItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    private String taskTitle;

    private ReviewerInfo reviewer;

    private Integer score;
    private String content;
    /** 0=发布者评接单者  1=接单者评发布者 */
    private Integer type;
    private Long createdAt;

    @Data
    public static class ReviewerInfo {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String nickname;
        private String avatar;
    }
}
