package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DBManager {

    private Connection getConnection() throws Exception {
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        return DriverManager.getConnection(
            "jdbc:derby://localhost:1527/pr2", "pr2", "pr2"
        );
    }

    // -------------------------------------------------------------------------
    // USER METHODS
    // -------------------------------------------------------------------------

    public boolean registerUser(String username, String name, String surname,
                                String email, String password) {
        String query = "INSERT INTO users (username, name, surname, email, password) "
                     + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setString(3, surname);
            pstmt.setString(4, email);
            pstmt.setString(5, password);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // VIDEO METHODS
    // -------------------------------------------------------------------------

    public boolean videoExists(int id) {
        String query = "SELECT id FROM videos WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerVideo(Video video) {
        String query = "INSERT INTO videos "
                     + "(id, title, author, creation_date, duration, views, "
                     + "description, format, file_path) "
                     + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, video.getId());
            pstmt.setString(2, video.getTitle());
            pstmt.setString(3, video.getAuthor());
            pstmt.setDate(4, java.sql.Date.valueOf(video.getCreationDate()));
            pstmt.setTime(5, java.sql.Time.valueOf(video.getDuration()));
            pstmt.setInt(6, video.getViews());
            pstmt.setString(7, video.getDescription());
            pstmt.setString(8, video.getFormat());
            pstmt.setString(9, video.getFilePath());

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Video> getAllVideos() {
        List<Video> videos = new ArrayList<>();
        String query = "SELECT * FROM videos";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalDate creationDate = rs.getDate("creation_date").toLocalDate();
                LocalTime duration     = rs.getTime("duration").toLocalTime();

                Video video = new Video(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    creationDate,
                    duration,
                    rs.getInt("views"),
                    rs.getString("description"),
                    rs.getString("format"),
                    rs.getString("file_path")
                );
                videos.add(video);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }
}