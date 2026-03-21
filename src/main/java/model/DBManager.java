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

    public boolean userExists(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

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

    public boolean videoExists(String title) {
        String query = "SELECT id FROM videos WHERE title = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerVideo(Video video) {
        String query = "INSERT INTO videos "
                     + "(title, author, creation_date, duration, views, "
                     + "description, format, file_path) "
                     + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, video.getTitle());
            pstmt.setString(2, video.getAuthor());
            pstmt.setDate(3, java.sql.Date.valueOf(video.getCreationDate()));
            pstmt.setTime(4, java.sql.Time.valueOf(video.getDuration()));
            pstmt.setInt(5, video.getViews());
            pstmt.setString(6, video.getDescription());
            pstmt.setString(7, video.getFormat());
            pstmt.setString(8, video.getFilePath());

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
    
    public boolean deleteVideo(int id, String author) {
        // The author check in the WHERE clause ensures users can only
        // delete their own videos, even if they craft a direct request
        String query = "DELETE FROM videos WHERE id = ? AND author = ?";
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, author);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // -------------------------------------------------------------------------
    // LIKE METHODS
    // -------------------------------------------------------------------------

    public boolean hasLiked(int videoId, String username) {
        String query = "SELECT 1 FROM likes WHERE video_id = ? AND username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, videoId);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean likeVideo(int videoId, String username) {
        String query = "INSERT INTO likes (video_id, username) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, videoId);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean unlikeVideo(int videoId, String username) {
        String query = "DELETE FROM likes WHERE video_id = ? AND username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, videoId);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Returns a map of videoId -> like count for all videos
    public java.util.Map<Integer, Integer> getLikeCounts() {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        String query = "SELECT video_id, COUNT(*) as total FROM likes GROUP BY video_id";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                counts.put(rs.getInt("video_id"), rs.getInt("total"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }

    // Returns the set of video IDs liked by a given user
    public java.util.Set<Integer> getLikedVideoIds(String username) {
        java.util.Set<Integer> liked = new java.util.HashSet<>();
        String query = "SELECT video_id FROM likes WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                liked.add(rs.getInt("video_id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return liked;
    }
}