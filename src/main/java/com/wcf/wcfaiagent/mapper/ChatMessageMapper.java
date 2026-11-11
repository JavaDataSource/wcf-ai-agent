package com.wcf.wcfaiagent.mapper;

import com.wcf.wcfaiagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话消息表 Mapper：SQL 全部在 resources/mapper/ChatMessageMapper.xml 中。
 */
@Mapper
public interface ChatMessageMapper {

    /** 批量插入消息（空列表由 Service 层拦截，不会走到这里） */
    int batchInsert(@Param("list") List<ChatMessage> messages);

    /** 查询某会话的全部消息（按自增 id 升序，即发送顺序） */
    List<ChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

    /** 删除某会话的全部消息 */
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
