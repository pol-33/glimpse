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
                     + "description, format, file_path, original_filename, file_source) "
                     + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            pstmt.setString(9, video.getOriginalFilename());
            pstmt.setString(10, video.getFileSource());

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteVideo(int id, String author) {
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

    /**
     * Fetches a single video by its primary key.
     * Used by PlayVideoServlet to retrieve the file path before streaming.
     * Returns null if the video does not exist or a DB error occurs.
     */
    public Video getVideoById(int id) {
        String query =
            "SELECT v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "       v.description, v.format, v.file_path, v.original_filename, v.file_source " +
            "FROM videos v WHERE v.id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                LocalDate creationDate = rs.getDate("creation_date").toLocalDate();
                LocalTime duration     = rs.getTime("duration").toLocalTime();
                return new Video(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    creationDate,
                    duration,
                    rs.getInt("views"),
                    rs.getString("description"),
                    rs.getString("format"),
                    rs.getString("file_path"),
                    rs.getString("original_filename"),
                    rs.getString("file_source")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns one page of videos with like counts and user-liked status,
     * all in a single query. Replaces the previous getAllVideos(),
     * getLikeCounts() and getLikedVideoIds() calls.
     *
     * @param page       zero-based page index
     * @param pageSize   number of rows per page
     * @param loggedUser username of the current user (for user_liked flag)
     */
    public List<Video> getVideosPage(int page, int pageSize, String loggedUser) {
        List<Video> videos = new ArrayList<>();

        // Single JOIN: one DB round-trip instead of three.
        // COUNT(l.username)  → like count per video (NULL rows from LEFT JOIN not counted)
        // SUM(CASE ...)      → 1 if loggedUser has liked this video, 0 otherwise
        // Derby pagination   → OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        String query =
            "SELECT v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "       v.description, v.format, v.file_path, v.original_filename, v.file_source, " +
            "       COUNT(l.username) AS like_count, " +
            "       SUM(CASE WHEN l.username = ? THEN 1 ELSE 0 END) AS user_liked " +
            "FROM videos v " +
            "LEFT JOIN likes l ON v.id = l.video_id " +
            "GROUP BY v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "         v.description, v.format, v.file_path, v.original_filename, v.file_source " +
            "ORDER BY v.id DESC " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, loggedUser);
            pstmt.setInt(2, page * pageSize);
            pstmt.setInt(3, pageSize);

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
                    rs.getString("file_path"),
                    rs.getString("original_filename"),
                    rs.getString("file_source")
                );
                video.setLikeCount(rs.getInt("like_count"));
                video.setUserLiked(rs.getInt("user_liked") > 0);
                videos.add(video);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    /** Total number of videos — needed to calculate the number of pages. */
    public int getVideoCount() {
        String query = "SELECT COUNT(*) FROM videos";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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
}
