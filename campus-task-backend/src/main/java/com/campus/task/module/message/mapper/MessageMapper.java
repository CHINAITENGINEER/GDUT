package com.campus.task.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.task.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id=#{userId} AND is_read=0")
    int countUnread(@Param("userId") Long userId);

    @Update("UPDATE message SET is_read=1 WHERE receiver_id=#{userId} AND is_read=0")
    void readAll(@Param("userId") Long userId);
}
