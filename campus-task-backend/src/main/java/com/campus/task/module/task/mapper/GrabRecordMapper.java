package com.campus.task.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.task.module.task.entity.GrabRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface GrabRecordMapper extends BaseMapper<GrabRecord> {

    /** 查询用户当日抢单次数 */
    @Select("SELECT COUNT(*) FROM grab_record WHERE user_id=#{userId} AND grab_date=#{date}")
    int countDailyGrab(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 查询用户当日对某任务是否已抢 */
    @Select("SELECT COUNT(*) FROM grab_record WHERE user_id=#{userId} AND task_id=#{taskId} AND grab_date=#{date}")
    int countTaskGrab(@Param("userId") Long userId, @Param("taskId") Long taskId, @Param("date") LocalDate date);
}
