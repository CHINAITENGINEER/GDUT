package com.campus.task.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.task.module.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    @Select("SELECT COUNT(*) FROM review WHERE task_id=#{taskId} AND reviewer_id=#{reviewerId}")
    int existsReview(@Param("taskId") Long taskId, @Param("reviewerId") Long reviewerId);
}
