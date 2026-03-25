package com.campus.task.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.task.module.task.entity.UserViolation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface UserViolationMapper extends BaseMapper<UserViolation> {

    /** 查询用户当日违规次数 */
    @Select("SELECT COUNT(*) FROM user_violation WHERE user_id=#{userId} AND violation_date=#{date}")
    int countDailyViolation(@Param("userId") Long userId, @Param("date") LocalDate date);
}
