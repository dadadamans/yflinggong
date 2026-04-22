package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.UserSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户会话 Mapper
 * 负责登录会话的创建、查询和删除
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {

    /**
     * 根据 token 查询当前有效会话对应的用户信息。
     * @param token 会话 token
     * @return 用户信息
     */
    @Select("select u.* from user_session s join app_user u on u.id = s.user_id " +
            "where s.token = #{token} and (s.expired_at is null or s.expired_at > current_timestamp) " +
            "order by s.id desc limit 1")
    Map<String, Object> selectUserByToken(@Param("token") String token);

    /**
     * 创建新的登录会话。
     * @param token 会话 token
     * @param userId 用户ID
     * @param roleType 角色类型
     * @param expiredAt 过期时间
     * @return 影响行数
     */
    @Insert("insert into user_session(token, user_id, role_type, expired_at) values (#{token}, #{userId}, #{roleType}, #{expiredAt})")
    int insertSession(@Param("token") String token, @Param("userId") Long userId, @Param("roleType") String roleType,
                      @Param("expiredAt") LocalDateTime expiredAt);

    /**
     * 删除指定 token 对应的会话。
     * @param token 会话 token
     * @return 影响行数
     */
    @Delete("delete from user_session where token = #{token}")
    int deleteSession(@Param("token") String token);
}
