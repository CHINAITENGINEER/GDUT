package com.campus.task.module.rally.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RallyCreateDTO {

    /** 1运动 2游戏 */
    @NotNull(message = "活动类型不能为空")
    @Min(value = 1, message = "活动类型错误")
    @Max(value = 2, message = "活动类型错误")
    private Integer type;

    @NotBlank(message = "活动标题不能为空")
    @Size(min = 2, max = 100, message = "活动标题长度2-100字")
    private String title;

    @NotNull(message = "召集人数不能为空")
    @Min(value = 1, message = "召集人数至少1人")
    @Max(value = 100, message = "召集人数最多100人")
    private Integer recruitCount;

    /** 发起时间（毫秒时间戳） */
    @NotNull(message = "发起时间不能为空")
    private Long startTime;

    @Size(max = 300, message = "备注最多300字")
    private String remark;
}
