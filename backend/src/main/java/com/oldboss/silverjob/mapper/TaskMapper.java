package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.common.TaskStatus;
import com.oldboss.silverjob.entity.Task;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("select t.*, p.total_score as publisher_total_score, p.comment_count as publisher_comment_count, " +
            "e.total_score as elderly_total_score, e.comment_count as elderly_comment_count " +
            "from task t " +
            "left join app_user p on t.publisher_id = p.id " +
            "left join app_user e on t.elderly_id = e.id " +
            "order by t.id asc")
    List<Map<String, Object>> selectTaskList();

    @Select("select * from task where status = #{status} order by id desc")
    List<Map<String, Object>> selectTasksByStatus(@Param("status") String status);

    @Select("select * from task where status = #{status} order by id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectTasksByStatusPage(@Param("status") String status,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);

    @Select("select count(*) from task where status = #{status}")
    long countTasksByStatus(@Param("status") String status);

    @Select("select t.*, p.total_score as publisher_total_score, p.comment_count as publisher_comment_count, " +
            "e.total_score as elderly_total_score, e.comment_count as elderly_comment_count " +
            "from task t " +
            "left join app_user p on t.publisher_id = p.id " +
            "left join app_user e on t.elderly_id = e.id " +
            "where t.status = '" + TaskStatus.WAITING + "' order by t.id asc")
    List<Map<String, Object>> selectWaitingTasks();

    @Select("select t.*, p.total_score as publisher_total_score, p.comment_count as publisher_comment_count, " +
            "e.total_score as elderly_total_score, e.comment_count as elderly_comment_count " +
            "from task t " +
            "left join app_user p on t.publisher_id = p.id " +
            "left join app_user e on t.elderly_id = e.id " +
            "where t.status = '" + TaskStatus.WAITING + "' order by t.id asc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectWaitingTasksPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from task where status = '" + TaskStatus.WAITING + "'")
    long countWaitingTasks();

    @Select({
            "<script>",
            "select t.*, p.total_score as publisher_total_score, p.comment_count as publisher_comment_count, ",
            "e.total_score as elderly_total_score, e.comment_count as elderly_comment_count ",
            "from task t ",
            "left join app_user p on t.publisher_id = p.id ",
            "left join app_user e on t.elderly_id = e.id ",
            "where t.status = '" + TaskStatus.WAITING + "' and t.publisher_id not in ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "order by t.id asc",
            "</script>"
    })
    List<Map<String, Object>> selectWaitingTasksExcludePublishers(@Param("ids") List<Long> ids);

    @Select({
            "<script>",
            "select t.*, p.total_score as publisher_total_score, p.comment_count as publisher_comment_count, ",
            "e.total_score as elderly_total_score, e.comment_count as elderly_comment_count ",
            "from task t ",
            "left join app_user p on t.publisher_id = p.id ",
            "left join app_user e on t.elderly_id = e.id ",
            "where t.status = '" + TaskStatus.WAITING + "' and t.publisher_id not in ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "order by t.id asc limit #{limit} offset #{offset}",
            "</script>"
    })
    List<Map<String, Object>> selectWaitingTasksExcludePublishersPage(@Param("ids") List<Long> ids,
                                                                      @Param("offset") int offset,
                                                                      @Param("limit") int limit);

    @Select({
            "<script>",
            "select count(*) from task ",
            "where status = '" + TaskStatus.WAITING + "' and publisher_id not in ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    long countWaitingTasksExcludePublishers(@Param("ids") List<Long> ids);

    @Select("select t.*, u.mobile as elderly_mobile, u.health_condition as elderly_health_condition, " +
            "u.total_score as elderly_total_score, u.comment_count as elderly_comment_count " +
            "from task t left join app_user u on t.elderly_id = u.id " +
            "where t.publisher_id = #{publisherId} order by t.id asc")
    List<Map<String, Object>> selectTasksByPublisherId(@Param("publisherId") Long publisherId);

    @Select("select t.*, u.mobile as elderly_mobile, u.health_condition as elderly_health_condition, " +
            "u.total_score as elderly_total_score, u.comment_count as elderly_comment_count " +
            "from task t left join app_user u on t.elderly_id = u.id " +
            "where t.publisher_id = #{publisherId} order by t.id asc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectTasksByPublisherIdPage(@Param("publisherId") Long publisherId,
                                                           @Param("offset") int offset,
                                                           @Param("limit") int limit);

    @Select("select count(*) from task where publisher_id = #{publisherId}")
    long countTasksByPublisherId(@Param("publisherId") Long publisherId);

    @Select("select t.*, u.mobile as elderly_mobile, u.health_condition as elderly_health_condition, " +
            "u.total_score as elderly_total_score, u.comment_count as elderly_comment_count " +
            "from task t left join app_user u on t.elderly_id = u.id " +
            "where t.publisher_id = #{publisherId} and t.status = #{status} order by t.id asc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectTasksByPublisherIdPageAndStatus(@Param("publisherId") Long publisherId,
                                                                    @Param("status") String status,
                                                                    @Param("offset") int offset,
                                                                    @Param("limit") int limit);

    @Select("select count(*) from task where publisher_id = #{publisherId} and status = #{status}")
    long countTasksByPublisherIdAndStatus(@Param("publisherId") Long publisherId,
                                          @Param("status") String status);

    @Select("select * from task where id = #{id}")
    Map<String, Object> selectTaskById(@Param("id") Long id);

    @Select("select * from task where publisher_id = #{publisherId} order by id desc")
    List<Map<String, Object>> selectOrdersByPublisherId(@Param("publisherId") Long publisherId);

    @Select("select * from task where publisher_id = #{publisherId} order by id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectOrdersByPublisherIdPage(@Param("publisherId") Long publisherId,
                                                            @Param("offset") int offset,
                                                            @Param("limit") int limit);

    @Select("select count(*) from task where publisher_id = #{publisherId}")
    long countOrdersByPublisherId(@Param("publisherId") Long publisherId);

    @Select("select * from task where publisher_id = #{publisherId} and status = #{status} order by id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectOrdersByPublisherIdPageAndStatus(@Param("publisherId") Long publisherId,
                                                                     @Param("status") String status,
                                                                     @Param("offset") int offset,
                                                                     @Param("limit") int limit);

    @Select("select count(*) from task where publisher_id = #{publisherId} and status = #{status}")
    long countOrdersByPublisherIdAndStatus(@Param("publisherId") Long publisherId, @Param("status") String status);

    @Select("select t.*, p.mobile as publisher_mobile " +
            "from task t left join app_user p on t.publisher_id = p.id " +
            "where t.elderly_id = #{elderlyId} order by t.id desc")
    List<Map<String, Object>> selectOrdersByElderlyId(@Param("elderlyId") Long elderlyId);

    @Select("select t.*, p.mobile as publisher_mobile " +
            "from task t left join app_user p on t.publisher_id = p.id " +
            "where t.elderly_id = #{elderlyId} order by t.id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectOrdersByElderlyIdPage(@Param("elderlyId") Long elderlyId,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    @Select("select count(*) from task where elderly_id = #{elderlyId}")
    long countOrdersByElderlyId(@Param("elderlyId") Long elderlyId);

    @Select("select t.*, p.mobile as publisher_mobile " +
            "from task t left join app_user p on t.publisher_id = p.id " +
            "where t.elderly_id = #{elderlyId} and t.status = #{status} order by t.id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectOrdersByElderlyIdPageAndStatus(@Param("elderlyId") Long elderlyId,
                                                                   @Param("status") String status,
                                                                   @Param("offset") int offset,
                                                                   @Param("limit") int limit);

    @Select("select count(*) from task where elderly_id = #{elderlyId} and status = #{status}")
    long countOrdersByElderlyIdAndStatus(@Param("elderlyId") Long elderlyId, @Param("status") String status);

    @Select("select * from task order by id desc")
    List<Map<String, Object>> selectAllOrders();

    @Select("select * from task order by id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectAllOrdersPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from task")
    long countAllOrders();

    @Select("select * from task where status = #{status} order by id desc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectAllOrdersPageByStatus(@Param("status") String status,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    @Select("select count(*) from task where status = #{status}")
    long countAllOrdersByStatus(@Param("status") String status);

    @Select("select t.*, p.total_score as publisher_total_score, p.comment_count as publisher_comment_count, " +
            "e.total_score as elderly_total_score, e.comment_count as elderly_comment_count " +
            "from task t " +
            "left join app_user p on t.publisher_id = p.id " +
            "left join app_user e on t.elderly_id = e.id " +
            "order by t.id asc limit #{limit} offset #{offset}")
    List<Map<String, Object>> selectTaskListPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from task")
    long countTaskList();

    @Select("select nextval('task_id_seq')")
    Long getNextOrderId();

    @Insert("insert into task(id, task_type, title, address, time_text, salary, content, publisher_id, publisher_name, " +
            "publisher_role, status, elderly_id, elderly_name) " +
            "values (nextval('task_id_seq'), #{taskType}, #{title}, #{address}, #{timeText}, #{salary}, #{content}, #{publisherId}, #{publisherName}, " +
            "#{publisherRole}, '" + TaskStatus.WAITING + "', null, null)")
    int insertTask(@Param("taskType") String taskType, @Param("title") String title, @Param("address") String address,
                   @Param("timeText") String timeText, @Param("salary") Integer salary, @Param("content") String content,
                   @Param("publisherId") Long publisherId, @Param("publisherName") String publisherName,
                   @Param("publisherRole") String publisherRole);

    @Update("update task set status = '" + TaskStatus.WORKING + "', elderly_id = #{elderlyId}, elderly_name = #{elderlyName}, " +
            "updated_at = current_timestamp where id = #{taskId} and status = '" + TaskStatus.WAITING + "'")
    int updateTaskToWorking(@Param("taskId") Long taskId, @Param("elderlyId") Long elderlyId, @Param("elderlyName") String elderlyName);

    @Update("update task set status = '" + TaskStatus.CANCELLED + "', settle_text = '已取消，无需结算', updated_at = current_timestamp where id = #{taskId}")
    int updateTaskToCancelled(@Param("taskId") Long taskId);

    @Update("update task set status = '" + TaskStatus.PENDING_PAYMENT + "', finish_time = current_timestamp, " +
            "updated_at = current_timestamp where id = #{taskId}")
    int updateTaskToPendingPayment(@Param("taskId") Long taskId);

    @Update("update task set status = '" + TaskStatus.DONE + "', settle_text = '已结算', updated_at = current_timestamp where id = #{taskId}")
    int updateTaskToDone(@Param("taskId") Long taskId);

    @Update("update task set status = '" + TaskStatus.APPLYING + "', elderly_id = #{elderlyId}, elderly_name = #{elderlyName}, " +
            "updated_at = current_timestamp where id = #{taskId} and status = '" + TaskStatus.WAITING + "'")
    int updateTaskToApplying(@Param("taskId") Long taskId, @Param("elderlyId") Long elderlyId, @Param("elderlyName") String elderlyName);

    @Update("update task set status = '" + TaskStatus.WORKING + "', updated_at = current_timestamp where id = #{taskId} and status = '" + TaskStatus.APPLYING + "'")
    int updateTaskApprove(@Param("taskId") Long taskId);

    @Update("update task set status = '" + TaskStatus.WAITING + "', elderly_id = null, elderly_name = null, " +
            "updated_at = current_timestamp where id = #{taskId} and status = '" + TaskStatus.APPLYING + "'")
    int updateTaskReject(@Param("taskId") Long taskId);

    @Delete("delete from task where id = #{taskId}")
    int deleteTaskById(@Param("taskId") Long taskId);

    @Update("update task set elderly_name = #{newName} where elderly_name = #{oldName}")
    int syncElderlyName(@Param("oldName") String oldName, @Param("newName") String newName);

    @Update("update task set publisher_name = #{newName} where publisher_name = #{oldName} and publisher_role = #{role}")
    int syncPublisherName(@Param("oldName") String oldName, @Param("newName") String newName, @Param("role") String role);

    // ==================== 统计查询方法 ====================
    // 这类复杂聚合查询不适合用 Wrapper，使用自定义 SQL 更直观
    // 将SQL集中放在Mapper层，便于维护和修改

    /**
     * 查询任务状态分布（饼图数据）
     * 用于统计不同状态的任务数量
     *
     * SQL: SELECT status, count(*) as count FROM task GROUP BY status
     *
     * @return List，包含每种状态的统计
     *         例如：[{status="waiting", count=10}, {status="done", count=5}]
     */
    @Select("select status, count(*) as count from task group by status")
    java.util.List<java.util.Map<String, Object>> selectStatusDistribution();

    /**
     * 查询任务类型分布
     * 用于统计不同类型的任务数量
     *
     * SQL: SELECT task_type as type, count(*) as count FROM task GROUP BY task_type
     *
     * @return List，包含每种类型的统计
     *         例如：[{type="陪伴", count=8}, {type="家务", count=3}]
     */
    @Select("select task_type as type, count(*) as count from task group by task_type")
    java.util.List<java.util.Map<String, Object>> selectTypeDistribution();

    /**
     * 查询任务发布趋势（过去7天）
     * 用于绘制折线图，展示7天内每天的任务发布数量
     *
     * SQL: SELECT date(created_at) as date, count(*) as count
     *      FROM task
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
            "from task " +
            "where created_at >= current_date - interval '7 days' " +
            "group by date(created_at) " +
            "order by date")
    java.util.List<java.util.Map<String, Object>> selectTaskTrend();
}
