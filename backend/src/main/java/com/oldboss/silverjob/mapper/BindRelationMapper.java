package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.BindRelation;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface BindRelationMapper extends BaseMapper<BindRelation> {

    @Select("select * from bind_relation where bind_code = #{bindCode}")
    Map<String, Object> selectByBindCode(@Param("bindCode") String bindCode);

    @Select("select * from bind_relation where elderly_user_id = #{elderlyUserId} order by id asc limit 1")
    Map<String, Object> selectByElderlyId(@Param("elderlyUserId") Long elderlyUserId);

    @Select("select child_user_id from bind_relation where elderly_user_id = #{elderlyUserId} and child_user_id is not null")
    List<Long> selectChildIdsByElderlyId(@Param("elderlyUserId") Long elderlyUserId);

    @Select("select * from bind_relation where child_user_id = #{childUserId} order by id asc limit 1")
    Map<String, Object> selectByChildId(@Param("childUserId") Long childUserId);

    @Select("select b.*, " +
            "e.nickname as elderly_nickname, e.real_name as elderly_real_name, e.mobile as elderly_mobile, " +
            "c.nickname as child_nickname, c.real_name as child_real_name, c.mobile as child_mobile, c.relation " +
            "from bind_relation b " +
            "left join app_user e on b.elderly_user_id = e.id " +
            "left join app_user c on b.child_user_id = c.id " +
            "order by b.id")
    List<Map<String, Object>> selectAllBinds();

    @Select("select b.*, " +
            "e.nickname as elderly_nickname, e.real_name as elderly_real_name, e.mobile as elderly_mobile, " +
            "c.nickname as child_nickname, c.real_name as child_real_name, c.mobile as child_mobile, c.relation " +
            "from bind_relation b " +
            "left join app_user e on b.elderly_user_id = e.id " +
            "left join app_user c on b.child_user_id = c.id " +
            "order by b.id limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectAllBindsPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from bind_relation")
    long countAllBinds();

    @Insert("insert into bind_relation(elderly_user_id, bind_code, child_user_id) values (#{elderlyUserId}, #{bindCode}, null)")
    int insertBindRelation(@Param("elderlyUserId") Long elderlyUserId, @Param("bindCode") String bindCode);

    @Update("update bind_relation set bind_code = #{bindCode}, code_created_at = current_timestamp, " +
            "confirmed = false, confirmed_at = null where elderly_user_id = #{elderlyUserId}")
    int updateBindCode(@Param("elderlyUserId") Long elderlyUserId, @Param("bindCode") String bindCode);

    @Update("update bind_relation set child_user_id = #{childUserId}, confirmed = true, confirmed_at = current_timestamp " +
            "where bind_code = #{bindCode} and elderly_user_id is not null and child_user_id is null")
    int confirmBind(@Param("childUserId") Long childUserId, @Param("bindCode") String bindCode);

    @Update("update bind_relation set elderly_user_id = null, bind_code = null, confirmed = false where elderly_user_id = #{userId}")
    int unbindByElderlyId(@Param("userId") Long userId);

    @Update("update bind_relation set child_user_id = null, confirmed = false where child_user_id = #{userId}")
    int unbindByChildId(@Param("userId") Long userId);
}
