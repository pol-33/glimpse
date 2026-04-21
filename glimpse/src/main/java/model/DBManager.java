package model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import security.PasswordSecurity;

public class DBManager {

    private static final String URL  = "jdbc:derby://localhost:1527/pr2";
    private static final String USER = "pr2";
    private static final String PASS = "pr2";
    private static final Path UPLOAD_DIR =
        Paths.get(System.getProperty("user.home") + File.separator + "glimpse-uploads")
             .toAbsolutePath().normalize();

    // -------------------------------------------------------------------------
    // CONNECTION
    // -------------------------------------------------------------------------

    private Connection getConnection() throws Exception {
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // -------------------------------------------------------------------------
    // USER METHODS
    // -------------------------------------------------------------------------

    public boolean userExists(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            return pstmt.executeQuery().next();
        } catch (Exception e) { e.printStackTrace(); return false; }
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
            pstmt.setString(5, PasswordSecurity.hashPassword(password));
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean validateLogin(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) return false;

            String storedPassword = rs.getString("password");
            boolean valid = PasswordSecurity.verifyPassword(password, storedPassword);

            // Transparently migrate legacy plaintext rows to a hash on next login.
            if (valid && PasswordSecurity.isLegacyPlaintext(storedPassword)) {
                upgradeLegacyPassword(conn, username, password);
            }
            return valid;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void upgradeLegacyPassword(Connection conn, String username, String password)
            throws Exception {
        String query = "UPDATE users SET password = ? WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, PasswordSecurity.hashPassword(password));
            pstmt.setString(2, username);
            pstmt.executeUpdate();
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
            return pstmt.executeQuery().next();
        } catch (Exception e) { e.printStackTrace(); return false; }
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
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean deleteVideo(int id, String author) {
        String selectQuery =
            "SELECT file_path, file_source FROM videos WHERE id = ? AND author = ?";
        String deleteQuery = "DELETE FROM videos WHERE id = ? AND author = ?";

        try (Connection conn = getConnection()) {
            String filePath = null;
            String fileSource = null;

            try (PreparedStatement select = conn.prepareStatement(selectQuery)) {
                select.setInt(1, id);
                select.setString(2, author);

                ResultSet rs = select.executeQuery();
                if (!rs.next()) return false;

                filePath = rs.getString("file_path");
                fileSource = rs.getString("file_source");
            }

            try (PreparedStatement delete = conn.prepareStatement(deleteQuery)) {
                delete.setInt(1, id);
                delete.setString(2, author);
                if (delete.executeUpdate() <= 0) return false;
            }

            if ("upload".equals(fileSource)) {
                deleteUploadedFile(filePath);
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void deleteUploadedFile(String storedName) {
        if (storedName == null || storedName.trim().isEmpty()) return;

        Path candidate = UPLOAD_DIR.resolve(storedName).normalize();
        if (!candidate.startsWith(UPLOAD_DIR)) {
            throw new IllegalArgumentException("Refusing to delete file outside upload directory.");
        }

        try {
            Files.deleteIfExists(candidate);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete uploaded video file: " + storedName, e);
        }
    }

    /**
     * Fetches a single video by its primary key, including like count and
     * whether the given user has liked it, all in one query (single DB round-trip).
     *
     * @param id         the video's primary key
     * @param loggedUser username of the current user (for user_liked flag)
     * @return the Video with like data populated, or null if not found
     */
    public Video getVideoById(int id) {
        return getVideoById(id, "");
    }

    public Video getVideoById(int id, String loggedUser) {
        String query =
            "SELECT v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "       v.description, v.format, v.file_path, v.original_filename, v.file_source, " +
            "       COUNT(l.username) AS like_count, " +
            "       SUM(CASE WHEN l.username = ? THEN 1 ELSE 0 END) AS user_liked " +
            "FROM videos v " +
            "LEFT JOIN likes l ON v.id = l.video_id " +
            "WHERE v.id = ? " +
            "GROUP BY v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "         v.description, v.format, v.file_path, v.original_filename, v.file_source";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, loggedUser);
            pstmt.setInt(2, id);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
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
                return video;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns one page of videos with like counts and user-liked status,
     * all in a single query. 
     *
     * @param page       zero-based page index
     * @param pageSize   number of rows per page
     * @param loggedUser username of the current user (for user_liked flag)
     */
    public List<Video> getVideosPage(int page, int pageSize, String loggedUser, String sort) {
        List<Video> videos = new ArrayList<>();
        String orderBy = orderByClause(sort);

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
            orderBy +
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

    public String normalizeSort(String sort) {
        if ("date_desc".equals(sort) || "date_asc".equals(sort)
                || "likes_desc".equals(sort) || "likes_asc".equals(sort)
                || "views_desc".equals(sort) || "views_asc".equals(sort)) {
            return sort;
        }
        return "date_desc";
    }

    private String orderByClause(String sort) {
        switch (normalizeSort(sort)) {
            case "likes_asc":
                return "ORDER BY like_count ASC, v.creation_date DESC, v.id DESC ";
            case "likes_desc":
                return "ORDER BY like_count DESC, v.creation_date DESC, v.id DESC ";
            case "views_asc":
                return "ORDER BY v.views ASC, v.creation_date DESC, v.id DESC ";
            case "views_desc":
                return "ORDER BY v.views DESC, v.creation_date DESC, v.id DESC ";
            case "date_asc":
                return "ORDER BY v.creation_date ASC, v.id ASC ";
            case "date_desc":
            default:
                return "ORDER BY v.creation_date DESC, v.id DESC ";
        }
    }

    /**
     * Enriches a list of Video objects (e.g. from REST search results) with
     * like data fetched from the local DB in a single query.
     */
    public void enrichWithLikes(List<Video> videos, String loggedUser) {
        if (videos == null || videos.isEmpty()) return;

        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < videos.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append(videos.get(i).getId());
        }

        String q =
            "SELECT video_id, COUNT(*) AS like_count, " +
            "       SUM(CASE WHEN username = ? THEN 1 ELSE 0 END) AS user_liked " +
            "FROM likes WHERE video_id IN (" + inClause + ") GROUP BY video_id";

        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(q)) {
            p.setString(1, loggedUser);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                int vid = rs.getInt("video_id");
                int cnt = rs.getInt("like_count");
                boolean ul = rs.getInt("user_liked") > 0;
                for (Video v : videos) {
                    if (v.getId() == vid) {
                        v.setLikeCount(cnt);
                        v.setUserLiked(ul);
                        break;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int getVideoCount() {
        String query = "SELECT COUNT(*) FROM videos";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
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
            return pstmt.executeQuery().next();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean likeVideo(int videoId, String username) {
        String query = "INSERT INTO likes (video_id, username) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, videoId);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean unlikeVideo(int videoId, String username) {
        String query = "DELETE FROM likes WHERE video_id = ? AND username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, videoId);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * Toggles the like status of a video for a user.
     * Returns: [newLikedState (boolean), newLikeCount (int)]
     * Used by LikeAjaxServlet for no-refresh like updates.
     */
    public int[] toggleLike(int videoId, String username) {
        boolean wasLiked = hasLiked(videoId, username);
        if (wasLiked) {
            unlikeVideo(videoId, username);
        } else {
            likeVideo(videoId, username);
        }
        boolean isNowLiked = !wasLiked;

        // Get current count
        String q = "SELECT COUNT(*) FROM likes WHERE video_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(q)) {
            p.setInt(1, videoId);
            ResultSet rs = p.executeQuery();
            if (rs.next()) {
                return new int[]{ isNowLiked ? 1 : 0, rs.getInt(1) };
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new int[]{ isNowLiked ? 1 : 0, 0 };
    }
}
