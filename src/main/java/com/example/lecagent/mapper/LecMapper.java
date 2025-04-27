package com.example.lecagent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LecMapper {
    @Select("SELECT COUNT(*) FROM session WHERE chatId = #{chatId}")
    int getChatId(Long chatId);

    @Insert("INSERT INTO session (userId, chatId) VALUES (#{userId}, #{chatId})")
    void storeChatId(Long userId, Long chatId);

    @Select("SELECT chatId FROM session WHERE userId = #{userId}")
    List<Long> getHistory(Long userId);

    @Delete("DELETE FROM session WHERE chatId = #{chatId} AND userId = #{userId}")
    void deleteHistory(Long chatId, Long userId);
}
