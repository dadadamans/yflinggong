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

@Component
public class CommentMapper {

    private final JdbcTemplate jdbcTemplate;

    public CommentMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Comment comment) {
        String sql = "INSERT INTO comment (task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, comment.getTaskId(), comment.getOrderId(), comment.getReviewerId(),
                comment.getReviewerName(), comment.getReviewerRole(), comment.getRevieweeId(),
                comment.getRevieweeName(), comment.getRating(), comment.getContent(), comment.getCommentType());
    }

    public List<CommentItemVO> findByRevieweeId(Long revieweeId) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at FROM comment WHERE reviewee_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new CommentRowMapper(), revieweeId);
    }

    public List<CommentItemVO> findByRevieweeIdPage(Long revieweeId, int offset, int limit) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at " +
                "FROM comment WHERE reviewee_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new CommentRowMapper(), revieweeId, limit, offset);
    }

    public List<CommentItemVO> findByTaskId(Long taskId) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at FROM comment WHERE task_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new CommentRowMapper(), taskId);
    }

    public List<CommentItemVO> findByTaskIdPage(Long taskId, int offset, int limit) {
        String sql = "SELECT id, task_id, order_id, reviewer_id, reviewer_name, reviewer_role, reviewee_id, reviewee_name, rating, content, comment_type, created_at " +
                "FROM comment WHERE task_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new CommentRowMapper(), taskId, limit, offset);
    }

    public List<Long> findRevieweeIdsByTaskId(Long taskId) {
        String sql = "SELECT DISTINCT reviewee_id FROM comment WHERE task_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, taskId);
    }

    public int deleteByTaskId(Long taskId) {
        String sql = "DELETE FROM comment WHERE task_id = ?";
        return jdbcTemplate.update(sql, taskId);
    }

    public Integer sumRatingByUserId(Long userId) {
        String sql = "SELECT COALESCE(SUM(rating), 0) FROM comment WHERE reviewee_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    public Integer countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE reviewee_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    public int countByRevieweeId(Long revieweeId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE reviewee_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, revieweeId);
        return count == null ? 0 : count;
    }

    public int countByTaskId(Long taskId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE task_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, taskId);
        return count == null ? 0 : count;
    }

    public boolean hasCommented(Long taskId, Long reviewerId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE task_id = ? AND reviewer_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, taskId, reviewerId);
        return count != null && count > 0;
    }

    private static class CommentRowMapper implements RowMapper<CommentItemVO> {
        @Override
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
