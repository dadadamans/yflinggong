package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 留言 Mapper
 * 负责绑定关系下留言记录的查询、写入和已读更新
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询绑定关系下的留言列表。
     * @param bindRelationId 绑定关系ID
     * @return 留言列表
     */
    @Select("select m.id, m.sender_user_id, m.sender_role, m.content, m.is_read, m.sent_at, " +
            "coalesce(u.real_name, u.nickname) as sender_name " +
            "from message m " +
            "left join app_user u on m.sender_user_id = u.id " +
            "where m.bind_relation_id = #{bindRelationId} order by m.id asc")
    List<Map<String, Object>> selectMessagesByBindRelationId(@Param("bindRelationId") Long bindRelationId);

    /**
     * 统计指定绑定关系下的未读留言数。
     * @param bindRelationId 绑定关系ID
     * @return 未读数量
     */
    @Select("select count(*) from message where bind_relation_id = #{bindRelationId} and is_read = false")
    int countUnread(@Param("bindRelationId") Long bindRelationId);

    /**
     * 新增一条留言。
     * @param bindRelationId 绑定关系ID
     * @param senderUserId 发送人ID
     * @param senderRole 发送人角色
     * @param content 留言内容
     * @return 影响行数
     */
    @Insert("insert into message(bind_relation_id, sender_user_id, sender_role, content, is_read, sent_at) " +
            "values (#{bindRelationId}, #{senderUserId}, #{senderRole}, #{content}, false, current_timestamp)")
    int insertMessage(@Param("bindRelationId") Long bindRelationId, @Param("senderUserId") Long senderUserId,
                      @Param("senderRole") String senderRole, @Param("content") String content);

    /**
     * 将留言标记为已读。
     * @param messageId 留言ID
     * @return 影响行数
     */
    @Update("update message set is_read = true where id = #{messageId}")
    int markAsRead(@Param("messageId") Long messageId);
}
