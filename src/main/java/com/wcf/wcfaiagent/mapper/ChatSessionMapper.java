package com.wcf.wcfaiagent.mapper;

import com.wcf.wcfaiagent.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话表 Mapper：SQL 全部在 resources/mapper/ChatSessionMapper.xml 中。
 */
@Mapper
public interface ChatSessionMapper {

    /** 新增会话 */
    int insert(ChatSession session);

    /** 更新会话标题（同时刷新 updated_at） */
    int updateTitle(@Param("id") String id, @Param("title") String title);

    /** 刷新会话最近活跃时间 */
    int updateTime(@Param("id") String id);

    /** 按主键查询 */
    ChatSession selectById(@Param("id") String id);

    /** 查询某应用的会话列表（按最近活跃倒序） */
    List<ChatSession> selectByAppKey(@Param("appKey") String appKey);

    /** 删除会话 */
    int deleteById(@Param("id") String id);
}
