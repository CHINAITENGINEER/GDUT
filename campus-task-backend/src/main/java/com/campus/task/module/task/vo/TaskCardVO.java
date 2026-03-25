package com.campus.task.module.task.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 任务卡片VO（列表展示）
 */
@Data
public class TaskCardVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private Integer category;
    private String categoryName;
    private BigDecimal amount;
    private Integer deliveryType;
    private Long deadline;
    private Integer status;
    private String statusName;
    private List<String> taskImages;
    private PublisherInfo publisher;
    private Long createdAt;

    @Data
    public static class PublisherInfo {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String nickname;
        private String avatar;
        private Integer creditScore;
    }
}
