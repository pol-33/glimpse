package isdcm.glimpse.rest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access layer for the glimpse-rest service.
 * Shares the same Derby DB as glimpse but is fully independent code —
 * no JARs, no imports shared with the glimpse project.
 */
public class DBManager {

    private Connection getConnection() throws Exception {
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        return DriverManager.getConnection(
            "jdbc:derby://localhost:1527/pr2", "pr2", "pr2"
        );
    }

    // ── Increment view counter ────────────────────────────────────────────────

    /**
     * Atomically increments the view counter of a video and returns
     * the new value, or -1 if the video does not exist or a DB error occurs.
     */
    public int incrementViews(int videoId) {
        try (Connection conn = getConnection()) {

            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE videos SET views = views + 1 WHERE id = ?")) {
                upd.setInt(1, videoId);
                if (upd.executeUpdate() == 0) return -1; // not found
            }

            try (PreparedStatement sel = conn.prepareStatement(
                    "SELECT views FROM videos WHERE id = ?")) {
                sel.setInt(1, videoId);
                ResultSet rs = sel.executeQuery();
                if (rs.next()) return rs.getInt("views");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Searches videos by any combination of criteria.
     *   - String params  → case-insensitive LIKE (substring match)
     *   - Integer params → exact match on the date component
     *   - null / blank   → no restriction on that field
     */
    public List<VideoInfo> searchVideos(String title, String author,
                                        Integer year, Integer month, Integer day) {
        List<VideoInfo> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT id, title, author, creation_date, duration, views, " +
            "       description, format, file_source " +
            "FROM videos WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (title  != null && !title.trim().isEmpty()) {
            sql.append(" AND LOWER(title)  LIKE LOWER(?)");
            params.add("%" + title.trim() + "%");
        }
        if (author != null && !author.trim().isEmpty()) {
            sql.append(" AND LOWER(author) LIKE LOWER(?)");
            params.add("%" + author.trim() + "%");
        }
        if (year  != null) { sql.append(" AND YEAR(creation_date)  = ?"); params.add(year);  }
        if (month != null) { sql.append(" AND MONTH(creation_date) = ?"); params.add(month); }
        if (day   != null) { sql.append(" AND DAY(creation_date)   = ?"); params.add(day);   }

        sql.append(" ORDER BY id DESC");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String) pstmt.setString(i + 1, (String) p);
                else                     pstmt.setInt(i + 1, (Integer) p);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(new VideoInfo(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getDate("creation_date").toString(),  // yyyy-MM-dd
                    rs.getTime("duration").toString(),        // HH:mm:ss
                    rs.getInt("views"),
                    rs.getString("description"),
                    rs.getString("format"),
                    rs.getString("file_source")              // "url" | "upload"
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // ── VideoInfo DTO ─────────────────────────────────────────────────────────

    /** Lightweight DTO used only within glimpse-rest. */
    public static class VideoInfo {
        public final int    id;
        public final String title;
        public final String author;
        public final String creationDate;  // "yyyy-MM-dd"
        public final String duration;      // "HH:mm:ss"
        public final int    views;
        public final String description;
        public final String format;
        public final String fileSource;    // "url" | "upload"

        VideoInfo(int id, String title, String author,
                  String creationDate, String duration, int views,
                  String description, String format, String fileSource) {
            this.id           = id;
            this.title        = title;
            this.author       = author;
            this.creationDate = creationDate;
            this.duration     = duration;
            this.views        = views;
            this.description  = description;
            this.format       = format;
            this.fileSource   = fileSource;
        }
    }
}
