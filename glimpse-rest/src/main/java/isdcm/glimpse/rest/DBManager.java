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

    // Increment view counter

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

    // Search

    /**
     * Searches videos by any combination of criteria.
     *   - String params  → case-insensitive LIKE (substring match)
     *   - Integer params → exact match on the date component
     *   - null / blank   → no restriction on that field
     */
    public SearchResult searchVideos(String title, String author,
                                     Integer year, Integer month, Integer day,
                                     int page, int pageSize, String sort) {
        List<VideoInfo> results = new ArrayList<>();
        String orderBy = orderByClause(sort);

        StringBuilder where = new StringBuilder(" FROM videos v WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (title  != null && !title.trim().isEmpty()) {
            where.append(" AND LOWER(v.title)  LIKE LOWER(?)");
            params.add("%" + title.trim() + "%");
        }
        if (author != null && !author.trim().isEmpty()) {
            where.append(" AND LOWER(v.author) LIKE LOWER(?)");
            params.add("%" + author.trim() + "%");
        }
        if (year  != null) { where.append(" AND YEAR(v.creation_date)  = ?"); params.add(year);  }
        if (month != null) { where.append(" AND MONTH(v.creation_date) = ?"); params.add(month); }
        if (day   != null) { where.append(" AND DAY(v.creation_date)   = ?"); params.add(day);   }

        String dataSql =
            "SELECT v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "       v.description, v.format, v.file_source, v.file_path, COUNT(l.username) AS like_count " +
            "FROM videos v LEFT JOIN likes l ON v.id = l.video_id " +
            where.toString().replaceFirst(" FROM videos v", "") +
            " GROUP BY v.id, v.title, v.author, v.creation_date, v.duration, v.views, " +
            "          v.description, v.format, v.file_source, v.file_path " +
            orderBy +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        String countSql = "SELECT COUNT(*)" + where;

        int total = 0;

        try (Connection conn = getConnection()) {
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                bindParams(countStmt, params);
                ResultSet rs = countStmt.executeQuery();
                if (rs.next()) total = rs.getInt(1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(dataSql)) {
                bindParams(pstmt, params);
                pstmt.setInt(params.size() + 1, page * pageSize);
                pstmt.setInt(params.size() + 2, pageSize);

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String fileSource = rs.getString("file_source");
                    String externalUrl = "url".equals(fileSource)
                        ? rs.getString("file_path")
                        : null;

                    results.add(new VideoInfo(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDate("creation_date").toString(),  // yyyy-MM-dd
                        rs.getTime("duration").toString(),        // HH:mm:ss
                        rs.getInt("views"),
                        rs.getString("description"),
                        rs.getString("format"),
                        fileSource,
                        externalUrl
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SearchResult(results, total, page, pageSize);
    }

    private void bindParams(PreparedStatement pstmt, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof String) pstmt.setString(i + 1, (String) p);
            else                     pstmt.setInt(i + 1, (Integer) p);
        }
    }

    private String normalizeSort(String sort) {
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

    // VideoInfo DTO

    public static class SearchResult {
        public final List<VideoInfo> items;
        public final int total;
        public final int page;
        public final int pageSize;

        SearchResult(List<VideoInfo> items, int total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
    }

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
        public final String externalUrl;   // only for URL-based videos

        VideoInfo(int id, String title, String author,
                  String creationDate, String duration, int views,
                  String description, String format, String fileSource,
                  String externalUrl) {
            this.id           = id;
            this.title        = title;
            this.author       = author;
            this.creationDate = creationDate;
            this.duration     = duration;
            this.views        = views;
            this.description  = description;
            this.format       = format;
            this.fileSource   = fileSource;
            this.externalUrl  = externalUrl;
        }
    }
}
