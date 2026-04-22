package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.Map;

/**
 * 用户 Mapper
 * 负责用户资料、账号状态、体检报告和评分字段的数据库操作
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from app_user where username = #{username}")
    Map<String, Object> selectByUsername(@Param("username") String username);

    @Select("select * from app_user where role_type = #{roleType} order by id asc limit 1")
    Map<String, Object> selectByRole(@Param("roleType") String roleType);

    @Select("select * from app_user where id = #{id}")
    Map<String, Object> selectUserById(@Param("id") Long id);

    @Select("select * from app_user order by id asc limit #{limit} offset #{offset}")
    java.util.List<User> selectUsersPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from app_user")
    long countUsers();

    @Select({
            "<script>",
            "select * from app_user",
            "<where>",
            "<if test='roleType != null and roleType != \"\"'>",
            "and role_type = #{roleType}",
            "</if>",
            "<if test='enabled != null'>",
            "and enabled = #{enabled}",
            "</if>",
            "<if test='healthStatus != null and healthStatus != \"\"'>",
            "and role_type = 'elderly' and health_report_status = #{healthStatus}",
            "</if>",
            "</where>",
            "order by id asc limit #{limit} offset #{offset}",
            "</script>"
    })
    java.util.List<User> selectUsersPageFiltered(@Param("offset") int offset,
                                                 @Param("limit") int limit,
                                                 @Param("roleType") String roleType,
                                                 @Param("enabled") Boolean enabled,
                                                 @Param("healthStatus") String healthStatus);

    @Select({
            "<script>",
            "select count(*) from app_user",
            "<where>",
            "<if test='roleType != null and roleType != \"\"'>",
            "and role_type = #{roleType}",
            "</if>",
            "<if test='enabled != null'>",
            "and enabled = #{enabled}",
            "</if>",
            "<if test='healthStatus != null and healthStatus != \"\"'>",
            "and role_type = 'elderly' and health_report_status = #{healthStatus}",
            "</if>",
            "</where>",
            "</script>"
    })
    long countUsersFiltered(@Param("roleType") String roleType,
                            @Param("enabled") Boolean enabled,
                            @Param("healthStatus") String healthStatus);

    @Insert("insert into app_user(username, password_hash, role_type, nickname, mobile, real_name, relation, enabled, created_at, updated_at) " +
            "values (#{username}, #{passwordHash}, #{roleType}, #{nickname}, #{mobile}, #{realName}, #{relation}, true, current_timestamp, current_timestamp)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Update("update app_user set password_hash = #{passwordHash}, updated_at = current_timestamp where id = #{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("update app_user set nickname = #{nickname}, real_name = #{realName}, mobile = #{mobile}, " +
            "gender = #{gender}, age = #{age}, city = #{city}, health_desc = #{healthDesc}, skill_tags = #{skillTags}, " +
            "emergency_contact = #{emergencyContact}, emergency_mobile = #{emergencyMobile}, relation = #{relation}, " +
            "remark = #{remark}, note = #{note}, font_size = #{fontSize}, updated_at = current_timestamp where id = #{id}")
    int updateUserFull(@Param("id") Long id, @Param("nickname") String nickname, @Param("realName") String realName,
                       @Param("mobile") String mobile, @Param("gender") String gender, @Param("age") Short age,
                       @Param("city") String city, @Param("healthDesc") String healthDesc, @Param("skillTags") String skillTags,
                       @Param("emergencyContact") String emergencyContact, @Param("emergencyMobile") String emergencyMobile,
                       @Param("relation") String relation, @Param("remark") String remark, @Param("note") String note,
                       @Param("fontSize") String fontSize);

    @Update("update app_user set enabled = #{enabled}, updated_at = current_timestamp where id = #{userId} and role_type != 'admin'")
    int setEnabled(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    @Update("update app_user set health_report_url = #{fileUrl}, health_report_status = #{status}, " +
            "health_report_time = current_timestamp, health_condition = #{condition} where id = #{userId}")
    int updateHealthReport(@Param("userId") Long userId, @Param("fileUrl") String fileUrl, @Param("status") String status, @Param("condition") String condition);

    @Update("update app_user set font_size = #{fontSize}, updated_at = current_timestamp where id = #{userId}")
    int updateFontSize(@Param("userId") Long userId, @Param("fontSize") String fontSize);

    @Update("update app_user set enabled = false from bind_relation br " +
            "where app_user.id = br.child_user_id and br.elderly_user_id = #{elderlyId}")
    int disableChildByElderlyId(@Param("elderlyId") Long elderlyId);

    @Update("update task set elderly_name = #{newName} where elderly_name = #{oldName}")
    int syncElderlyName(@Param("oldName") String oldName, @Param("newName") String newName);

    @Update("update task set publisher_name = #{newName} where publisher_name = #{oldName} and publisher_role = #{role}")
    int syncPublisherName(@Param("oldName") String oldName, @Param("newName") String newName, @Param("role") String role);

    // ==================== 统计查询方法 ====================
    // 这类复杂聚合查询不适合用 Wrapper，使用自定义 SQL 更直观
    // 将SQL集中放在Mapper层，便于维护和修改

    /**
     * 查询用户注册趋势（过去7天）
     * 用于绘制折线图，展示7天内每天的新用户注册数量
     *
     * SQL: SELECT date(created_at) as date, count(*) as count
     *      FROM app_user
     *      WHERE created_at >= current_date - interval '7 days'
     *      GROUP BY date(created_at)
     *      ORDER BY date
     *
     * 注意：PostgreSQL 的 interval '7 days' 语法，其他数据库需调整
     *
     * @return List，按日期排序的趋势数据
     *         例如：[{date="2024-01-01", count=5}, {date="2024-01-02", count=3}]
     */
    @Select("select date(created_at) as date, count(*) as count " +
            "from app_user " +
            "where created_at >= current_date - interval '7 days' " +
            "group by date(created_at) " +
            "order by date")
    java.util.List<java.util.Map<String, Object>> selectUserTrend();

    @Update("update app_user set total_score = #{totalScore}, comment_count = #{commentCount}, updated_at = current_timestamp where id = #{userId}")
    int updateUserRating(@Param("userId") Long userId, @Param("totalScore") Integer totalScore, @Param("commentCount") Integer commentCount);
}
