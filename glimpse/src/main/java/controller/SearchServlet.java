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
import model.DBManager;
import model.Video;

/**
 * Calls glimpse-rest GET /resources/videos/search, then enriches the results
 * with like data from the local DB (via enrichWithLikes), and forwards to
 * searchResults.jsp which reuses the same videoTable.jsp fragment as listVideos.jsp.
 */
@WebServlet(name = "SearchServlet", urlPatterns = {"/SearchServlet"})
public class SearchServlet extends HttpServlet {

    private static final String REST_SEARCH_URL =
        "http://localhost:8080/glimpse-rest/resources/videos/search";

    private static final int TIMEOUT_MS = 5_000;
    private static final int PAGE_SIZE = 20;
    private static final String DEFAULT_SORT = "date_desc";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // Session guard
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String loggedUser = (String) session.getAttribute("loggedUser");
        
        // Read and trim search parameters
        String title  = emptyToNull(request.getParameter("title"));
        String author = emptyToNull(request.getParameter("author"));
        String year   = emptyToNull(request.getParameter("year"));
        String month  = emptyToNull(request.getParameter("month"));
        String day    = emptyToNull(request.getParameter("day"));
        int page = parsePage(request.getParameter("page"));
        String sort = normalizeSort(request.getParameter("sort"));

        // Require at least one criterion
        if (title == null && author == null && year == null && month == null && day == null) {
            request.setAttribute("searchError", "Please fill in at least one search field.");
            request.setAttribute("videos", new ArrayList<Video>());
            request.setAttribute("currentPage", 0);
            request.setAttribute("totalPages", 0);
            request.setAttribute("totalVideos", 0);
            echoParams(request, title, author, year, month, day);
            request.getRequestDispatcher("searchResults.jsp").forward(request, response);
            return;
        }

        // Build REST URL
        StringBuilder urlSb = new StringBuilder(REST_SEARCH_URL).append("?1=1");
        appendEncoded(urlSb, "title",  title);
        appendEncoded(urlSb, "author", author);
        appendEncoded(urlSb, "year",   year);
        appendEncoded(urlSb, "month",  month);
        appendEncoded(urlSb, "day",    day);
        appendEncoded(urlSb, "page",   String.valueOf(page));
        appendEncoded(urlSb, "pageSize", String.valueOf(PAGE_SIZE));
        appendEncoded(urlSb, "sort", sort);

        // Call REST service via HttpURLConnection
        SearchResponse searchResponse = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlSb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    searchResponse = parseSearchResponse(json);
                }
            }
        } catch (Exception e) {
            // Connection refused, timeout, etc.
            searchResponse = null;
        } finally {
            if (conn != null) conn.disconnect();
        }

        // Enrich with likes from local DB
        if (searchResponse != null) {
            new DBManager().enrichWithLikes(searchResponse.items, loggedUser);
        }

        if (searchResponse == null) {
            request.setAttribute("searchError",
                "The search service is temporarily unavailable. " +
                "Make sure glimpse-rest is running and try again.");
            request.setAttribute("videos", new ArrayList<Video>());
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", 0);
            request.setAttribute("totalVideos", 0);
        } else {
            int totalPages = (int) Math.ceil((double) searchResponse.total / PAGE_SIZE);
            request.setAttribute("videos", searchResponse.items);
            request.setAttribute("currentPage", searchResponse.page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalVideos", searchResponse.total);
        }

        request.setAttribute("currentSort", sort);
        echoParams(request, title, author, year, month, day);
        request.getRequestDispatcher("searchResults.jsp").forward(request, response);
    }

    // JSON parser

    private SearchResponse parseSearchResponse(String json) {
        SearchResponse response = new SearchResponse();
        if (json == null) return response;
        json = json.trim();
        if (json.isEmpty()) return response;

        response.page = toInt(getField(json, "page"));
        response.pageSize = toInt(getField(json, "pageSize"));
        response.total = toInt(getField(json, "total"));
        response.items = parseItemsArray(getArrayField(json, "items"));
        return response;
    }

    private List<Video> parseItemsArray(String json) {
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
            String externalUrl = getField(obj, "externalUrl");

            if (dateStr == null || durStr == null) return null;
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime dur  = LocalTime.parse(durStr);

            return new Video(id, title, author, date, dur, views,
                             desc, format,
                             externalUrl != null ? externalUrl : "",
                             null, fileSource);
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

    private String getArrayField(String obj, String key) {
        String search = "\"" + key + "\":";
        int idx = obj.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        while (idx < obj.length() && Character.isWhitespace(obj.charAt(idx))) idx++;
        if (idx >= obj.length() || obj.charAt(idx) != '[') return null;

        int depth = 0;
        for (int i = idx; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return obj.substring(idx, i + 1);
            }
        }
        return null;
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
        req.setAttribute("searchBaseUrl", buildSearchBaseUrl(
            title, author, year, month, day,
            (String) req.getAttribute("currentSort")
        ));
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private int toInt(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private int parsePage(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String buildSearchBaseUrl(String title, String author,
                                      String year, String month, String day,
                                      String sort) {
        StringBuilder sb = new StringBuilder("SearchServlet?1=1");
        appendEncoded(sb, "title", title);
        appendEncoded(sb, "author", author);
        appendEncoded(sb, "year", year);
        appendEncoded(sb, "month", month);
        appendEncoded(sb, "day", day);
        appendEncoded(sb, "sort", normalizeSort(sort));
        return sb.toString();
    }

    private String normalizeSort(String sort) {
        if ("date_desc".equals(sort) || "date_asc".equals(sort)
                || "likes_desc".equals(sort) || "likes_asc".equals(sort)
                || "views_desc".equals(sort) || "views_asc".equals(sort)) {
            return sort;
        }
        return DEFAULT_SORT;
    }

    private static class SearchResponse {
        private List<Video> items = new ArrayList<>();
        private int total;
        private int page;
        private int pageSize;
    }
}
