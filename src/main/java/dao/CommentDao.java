package dao;

import model.Comment;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommentDao {

        public void save(Comment c) {
            String sql = "INSERT INTO comments(task_id, user_id, content) VALUES (?, ?, ?)";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setLong(1, c.getTaskId());
                ps.setLong(2, c.getUserId());
                ps.setString(3, c.getContent());

                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    public List<Comment> findByTaskId(long taskId) {
        String sql = "SELECT * FROM comments WHERE task_id = ?";

        List<Comment> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, taskId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Comment cm = new Comment();
                cm.setId(rs.getLong("comment_id"));
                cm.setTaskId(rs.getLong("task_id"));
                cm.setUserId(rs.getLong("user_id"));
                cm.setContent(rs.getString("content"));
                list.add(cm);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


}
