package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.UserSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {

    @Select("select u.* from user_session s join app_user u on u.id = s.user_id " +
            "where s.token = #{token} and (s.expired_at is null or s.expired_at > current_timestamp) " +
            "order by s.id desc limit 1")
    Map<String, Object> selectUserByToken(@Param("token") String token);

    @Insert("insert into user_session(token, user_id, role_type, expired_at) values (#{token}, #{userId}, #{roleType}, #{expiredAt})")
    int insertSession(@Param("token") String token, @Param("userId") Long userId, @Param("roleType") String roleType,
                      @Param("expiredAt") LocalDateTime expiredAt);

    @Delete("delete from user_session where token = #{token}")
    int deleteSession(@Param("token") String token);
}
