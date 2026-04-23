package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.Feedback;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Insert("insert into feedback(user_id, role, content, related_order_id, status, created_at) " +
            "values (#{userId}, #{role}, #{content}, #{relatedOrderId}, 'pending', current_timestamp)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFeedback(Feedback feedback);

    @Select("select f.id, f.user_id, f.role, f.content, f.related_order_id, f.status, f.created_at, " +
            "coalesce(u.real_name, u.nickname) as user_name, t.title as order_title " +
            "from feedback f " +
            "left join app_user u on f.user_id = u.id " +
            "left join task t on f.related_order_id = t.id " +
            "order by f.id desc")
    List<Map<String, Object>> selectAllFeedback();

    @Select("select f.id, f.user_id, f.role, f.content, f.related_order_id, f.status, f.created_at, " +
            "t.title as order_title " +
            "from feedback f " +
            "left join task t on f.related_order_id = t.id " +
            "where f.user_id = #{userId} " +
            "order by f.id desc")
    List<Map<String, Object>> selectFeedbackByUserId(@Param("userId") Long userId);

    @Update("update feedback set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}