package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.Video;

/**
 * Calls glimpse-rest GET /resources/videos/search and forwards results to
 * searchResults.jsp.
 *
 * Uses plain java.net.HttpURLConnection (java.base module — no extra deps).
 * Parses the JSON response with a hand-rolled parser to avoid jakarta.json
 * dependency issues in some GlassFish configurations.
 *
 * If glimpse-rest is unreachable, the user sees a friendly error message
 * instead of a 500 crash.
 *
 * URL: GET /SearchServlet?title=X&author=Y&year=2026&month=3&day=19
 */
@WebServlet(name = "SearchServlet", urlPatterns = {"/SearchServlet"})
public class SearchServlet extends HttpServlet {

    private static final String REST_SEARCH_URL =
        "http://localhost:8080/glimpse-rest/resources/videos/search";

    private static final int TIMEOUT_MS = 5_000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // ── Session guard ──────────────────────────────────────────────────
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        // ── Read and trim search parameters ────────────────────────────────
        String title  = emptyToNull(request.getParameter("title"));
        String author = emptyToNull(request.getParameter("author"));
        String year   = emptyToNull(request.getParameter("year"));
        String month  = emptyToNull(request.getParameter("month"));
        String day    = emptyToNull(request.getParameter("day"));

        // Require at least one criterion
        if (title == null && author == null && year == null
                && month == null && day == null) {
            request.setAttribute("searchError",
                "Please fill in at least one search field.");
            request.setAttribute("videos", new ArrayList<Video>());
            echoParams(request, title, author, year, month, day);
            request.getRequestDispatcher("searchResults.jsp").forward(request, response);
            return;
        }

        // ── Build REST URL ─────────────────────────────────────────────────
        StringBuilder urlSb = new StringBuilder(REST_SEARCH_URL).append("?1=1");
        appendEncoded(urlSb, "title",  title);
        appendEncoded(urlSb, "author", author);
        appendEncoded(urlSb, "year",   year);
        appendEncoded(urlSb, "month",  month);
        appendEncoded(urlSb, "day",    day);

        // ── Call REST service via HttpURLConnection ─────────────────────────
        List<Video> videos = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlSb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int status = conn.getResponseCode();
            if (status == 200) {
                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    videos = parseJsonArray(json);
                }
            } else {
                // REST returned an error code — treat as service failure
                videos = null;
            }
        } catch (Exception e) {
            // Connection refused, timeout, etc.
            videos = null;
        } finally {
            if (conn != null) conn.disconnect();
        }

        // ── Forward to view ────────────────────────────────────────────────
        if (videos == null) {
            request.setAttribute("searchError",
                "The search service is temporarily unavailable. " +
                "Please make sure glimpse-rest is running and try again.");
            request.setAttribute("videos", new ArrayList<Video>());
        } else {
            request.setAttribute("videos", videos);
        }

        echoParams(request, title, author, year, month, day);
        request.getRequestDispatcher("searchResults.jsp").forward(request, response);
    }

    // ── JSON parsing — no external library ────────────────────────────────────

    /**
     * Parses a JSON array of video objects returned by glimpse-rest.
     * The format is completely predictable so a hand-rolled parser is safe here.
     */
    private List<Video> parseJsonArray(String json) {
        List<Video> list = new ArrayList<>();
        if (json == null) return list;
        json = json.trim();
        if (json.equals("[]") || json.isEmpty()) return list;

        // Iterate over each {...} object in the top-level array
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    Video v = parseVideoObject(json.substring(start, i + 1));
                    if (v != null) list.add(v);
                    start = -1;
                }
            }
        }
        return list;
    }

    /**
     * Parses one JSON object like
     * {"id":1,"title":"foo","author":"bar","creationDate":"2026-03-01",
     *  "duration":"00:05:00","views":3,"description":"desc",
     *  "format":"mp4","fileSource":"upload"}
     * into a Video instance. Returns null on any parse error.
     */
    private Video parseVideoObject(String obj) {
        try {
            int    id         = toInt(getField(obj, "id"));
            String title      = getField(obj, "title");
            String author     = getField(obj, "author");
            String dateStr    = getField(obj, "creationDate");
            String durStr     = getField(obj, "duration");
            int    views      = toInt(getField(obj, "views"));
            String desc       = getField(obj, "description");
            String format     = getField(obj, "format");
            String fileSource = getField(obj, "fileSource");

            if (dateStr == null || durStr == null) return null;
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime dur  = LocalTime.parse(durStr);

            // filePath is intentionally not returned by glimpse-rest
            // (internal paths should not be exposed). PlayVideoServlet
            // fetches it from glimpse's own DB when needed.
            return new Video(id, title, author, date, dur, views,
                             desc, format, "", null, fileSource);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the raw value (unquoted string or number) for a given JSON key
     * from a flat JSON object string.  Handles:
     *   - String values:  "key":"value"  (with basic escape handling)
     *   - Numeric values: "key":42
     *   - Null values:    "key":null     → returns null
     */
    private String getField(String obj, String key) {
        String search = "\"" + key + "\":";
        int idx = obj.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        if (idx >= obj.length()) return null;

        char first = obj.charAt(idx);

        if (first == 'n') return null;           // null literal

        if (first == '"') {
            // String: scan until closing unescaped quote
            StringBuilder sb = new StringBuilder();
            int i = idx + 1;
            while (i < obj.length()) {
                char ch = obj.charAt(i);
                if (ch == '\\' && i + 1 < obj.length()) {
                    char next = obj.charAt(i + 1);
                    switch (next) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        default:   sb.append(next); break;
                    }
                    i += 2;
                } else if (ch == '"') {
                    break;
                } else {
                    sb.append(ch);
                    i++;
                }
            }
            return sb.toString();
        }

        // Numeric / boolean: read until , or }
        int end = idx;
        while (end < obj.length() && obj.charAt(end) != ',' && obj.charAt(end) != '}') end++;
        return obj.substring(idx, end).trim();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendEncoded(StringBuilder sb, String key, String value) {
        if (value == null) return;
        sb.append("&").append(key).append("=")
          .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private void echoParams(HttpServletRequest req, String title, String author,
                             String year, String month, String day) {
        req.setAttribute("q_title",  title  != null ? title  : "");
        req.setAttribute("q_author", author != null ? author : "");
        req.setAttribute("q_year",   year   != null ? year   : "");
        req.setAttribute("q_month",  month  != null ? month  : "");
        req.setAttribute("q_day",    day    != null ? day    : "");
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private int toInt(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
