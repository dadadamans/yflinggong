package com.oldboss.silverjob.mapper;

import com.oldboss.silverjob.entity.Comment;
import com.oldboss.silverjob.vo.CommentItemVO;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * 评价 Mapper
 * 使用 JdbcTemplate 执行评价表的增删查和评分统计
 */
@Component
public class CommentMapper {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造评价 Mapper 并注入 JdbcTemplate。
     * @param jdbcTemplate JDBC 操作对象
     */
    public CommentMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增一条评价记录。
     * @param comment 评价实体
     */
    public void insert(Comment comment) {
        String sql = "INSERT INTO comment (task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, comment.getTaskId(), comment.getOrderId(), comment.getReviewerId(),
                comment.getReviewerName(), comment.getReviewerRole(), comment.getRevieweeId(),
                comment.getRevieweeName(), comment.getRating(), comment.getContent(), comment.getCommentType());
    }

    /**
     * 查询某个用户收到的全部评价。
     * @param revieweeId 被评价人ID
     * @return 评价列表
     */
    public List<CommentItemVO> findByRevieweeId(Long revieweeId) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at FROM comment WHERE reviewee_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new CommentRowMapper(), revieweeId);
    }

    /**
     * 分页查询某个用户收到的评价。
     * @param revieweeId 被评价人ID
     * @param offset 偏移量
     * @param limit 条数
     * @return 评价列表
     */
    public List<CommentItemVO> findByRevieweeIdPage(Long revieweeId, int offset, int limit) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at " +
                "FROM comment WHERE reviewee_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new CommentRowMapper(), revieweeId, limit, offset);
    }

    /**
     * 查询某个任务下的全部评价。
     * @param taskId 任务ID
     * @return 评价列表
     */
    public List<CommentItemVO> findByTaskId(Long taskId) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at FROM comment WHERE task_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new CommentRowMapper(), taskId);
    }

    /**
     * 分页查询某个任务下的评价。
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit 条数
     * @return 评价列表
     */
    public List<CommentItemVO> findByTaskIdPage(Long taskId, int offset, int limit) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at " +
                "FROM comment WHERE task_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new CommentRowMapper(), taskId, limit, offset);
    }

    /**
     * 查询某个任务评价中涉及到的被评价人ID集合。
     * @param taskId 任务ID
     * @return 被评价人ID列表
     */
    public List<Long> findRevieweeIdsByTaskId(Long taskId) {
        String sql = "SELECT DISTINCT reviewee_id FROM comment WHERE task_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, taskId);
    }

    /**
     * 删除指定任务下的所有评价。
     * @param taskId 任务ID
     * @return 影响行数
     */
    public int deleteByTaskId(Long taskId) {
        String sql = "DELETE FROM comment WHERE task_id = ?";
        return jdbcTemplate.update(sql, taskId);
    }

    /**
     * 统计指定用户收到评价的总分。
     * @param userId 用户ID
     * @return 总分
     */
    public Integer sumRatingByUserId(Long userId) {
        String sql = "SELECT COALESCE(SUM(rating), 0) FROM comment WHERE reviewee_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    /**
     * 统计指定用户收到的评价数量。
     * @param userId 用户ID
     * @return 评价数量
     */
    public Integer countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE reviewee_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    /**
     * 统计指定用户收到评价的总条数。
     * @param revieweeId 被评价人ID
     * @return 评价条数
     */
    public int countByRevieweeId(Long revieweeId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE reviewee_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, revieweeId);
        return count == null ? 0 : count;
    }

    /**
     * 统计指定任务下评价总条数。
     * @param taskId 任务ID
     * @return 评价条数
     */
    public int countByTaskId(Long taskId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE task_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, taskId);
        return count == null ? 0 : count;
    }

    /**
     * 判断某个用户是否已经评价过指定任务。
     * @param taskId 任务ID
     * @param reviewerId 评价人ID
     * @return 是否已评价
     */
    public boolean hasCommented(Long taskId, Long reviewerId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE task_id = ? AND reviewer_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, taskId, reviewerId);
        return count != null && count > 0;
    }

    /**
     * 将结果集映射为评价展示对象。
     */
    private static class CommentRowMapper implements RowMapper<CommentItemVO> {
        @Override
        /**
         * 将结果集当前行映射为评价对象。
         * @param rs 结果集
         * @param rowNum 当前行号
         * @return 评价对象
         * @throws SQLException SQL 读取异常
         */
        public CommentItemVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            CommentItemVO vo = new CommentItemVO();
            vo.setId(rs.getLong("id"));
            vo.setTaskId(rs.getLong("task_id"));
            vo.setOrderId(rs.getLong("order_id"));
            vo.setReviewerId(rs.getLong("reviewer_id"));
            vo.setReviewerName(rs.getString("reviewer_name"));
            vo.setReviewerRole(rs.getString("reviewer_role"));
            vo.setRevieweeId(rs.getLong("reviewee_id"));
            vo.setRevieweeName(rs.getString("reviewee_name"));
            vo.setRating(rs.getInt("rating"));
            vo.setContent(rs.getString("content"));
            vo.setCommentType(rs.getString("comment_type"));
            vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return vo;
        }
    }
}
