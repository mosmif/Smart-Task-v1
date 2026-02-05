package dao;

import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AttachementDao {

    public void saveAttachment(long taskId, String filename, String path) {

        String sql = "INSERT INTO attachments(task_id, filename, filepath) VALUES (?, ?, ?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, taskId);
            ps.setString(2, filename);
            ps.setString(3, path);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
