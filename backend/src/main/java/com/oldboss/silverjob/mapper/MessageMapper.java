package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("select m.id, m.sender_user_id, m.sender_role, m.content, m.is_read, m.sent_at, " +
            "coalesce(u.real_name, u.nickname) as sender_name " +
            "from message m " +
            "left join app_user u on m.sender_user_id = u.id " +
            "where m.bind_relation_id = #{bindRelationId} order by m.id asc")
    List<Map<String, Object>> selectMessagesByBindRelationId(@Param("bindRelationId") Long bindRelationId);

    @Select("select count(*) from message where bind_relation_id = #{bindRelationId} and is_read = false")
    int countUnread(@Param("bindRelationId") Long bindRelationId);

    @Insert("insert into message(bind_relation_id, sender_user_id, sender_role, content, is_read, sent_at) " +
            "values (#{bindRelationId}, #{senderUserId}, #{senderRole}, #{content}, false, current_timestamp)")
    int insertMessage(@Param("bindRelationId") Long bindRelationId, @Param("senderUserId") Long senderUserId,
                      @Param("senderRole") String senderRole, @Param("content") String content);

    @Update("update message set is_read = true where id = #{messageId}")
    int markAsRead(@Param("messageId") Long messageId);
}