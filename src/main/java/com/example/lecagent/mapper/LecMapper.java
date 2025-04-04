package com.example.lecagent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LecMapper {
    @Select("SELECT COUNT(*) FROM session WHERE chatId = #{chatId}")
    int getChatId(Long chatId);

    @Insert("INSERT INTO session (userId, chatId) VALUES (#{userId}, #{chatId})")
    void storeChatId(Long userId, Long chatId);
}
