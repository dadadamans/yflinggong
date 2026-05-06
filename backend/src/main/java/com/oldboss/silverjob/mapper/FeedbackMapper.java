package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.Feedback;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("""
        SELECT f.id, f.user_id, f.role, f.content, f.feedback_type,
               f.related_order_id, f.status, f.admin_reply, f.handled_by,
               f.created_at, f.updated_at,
               COALESCE(u.real_name, u.nickname) as user_name,
               t.title as order_title
        FROM feedback f
        LEFT JOIN app_user u ON f.user_id = u.id
        LEFT JOIN task t ON f.related_order_id = t.id
        ORDER BY f.id DESC
    """)
    List<Map<String, Object>> selectAllWithDetail();

    @Select("""
        SELECT f.id, f.user_id, f.role, f.content, f.feedback_type,
               f.related_order_id, f.status, f.admin_reply, f.handled_by,
               f.created_at, f.updated_at,
               COALESCE(u.real_name, u.nickname) as user_name,
               t.title as order_title
        FROM feedback f
        LEFT JOIN app_user u ON f.user_id = u.id
        LEFT JOIN task t ON f.related_order_id = t.id
        WHERE f.user_id = #{userId}
        ORDER BY f.id DESC
    """)
    List<Map<String, Object>> selectByUserIdWithDetail(@Param("userId") Long userId);
}