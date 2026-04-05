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
import model.DBManager;
import model.Video;

/**
 * Handles the "Play" action for uploaded videos.
 *
 * Flow:
 *   1. Session guard.
 *   2. Look up the video in glimpse's DB using the new getVideoById(id, loggedUser)
 *      which returns like data in the same JOIN.
 *   3. Verify it is an uploaded file (URL-based videos cannot be streamed).
 *   4. Call PUT /resources/videos/{id}/views on glimpse-rest (non-blocking) to
 *      increment the view counter.
 *   5. Store updated view count (or -1 if REST is offline) in a request attribute
 *      and forward to playVideo.jsp.
 *
 * URL pattern: GET /PlayVideoServlet?id=<videoId>
 */
@WebServlet(name = "PlayVideoServlet", urlPatterns = {"/PlayVideoServlet"})
public class PlayVideoServlet extends HttpServlet {

    /** Must match the glimpse-rest deployment. */
    private static final String REST_VIEWS_BASE =
        "http://localhost:8080/glimpse-rest/resources/videos/";

    private static final int TIMEOUT_MS = 4_000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Session guard
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String loggedUser = (String) session.getAttribute("loggedUser");

        // Parse video id
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid video id.");
            return;
        }

        // Load video from glimpse's DB
        DBManager db = new DBManager();
        Video video = db.getVideoById(id, loggedUser);

        if (video == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not found.");
            return;
        }

        // Only uploaded videos can be played on our server
        if (!"upload".equals(video.getFileSource())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Only locally uploaded videos can be played here.");
            return;
        }

        // Call REST PUT to increment views
        // Uses java.net.HttpURLConnection (java.base, no extra modules needed).
        // If glimpse-rest is offline we continue anyway; the view count will
        // simply not be updated and a warning is shown on the player page.
        int updatedViews = callRestPut(id);
        request.setAttribute("updatedViews", updatedViews); // -1 = REST offline
        
        // Forward to the player view
        request.setAttribute("video", video);
        request.getRequestDispatcher("playVideo.jsp").forward(request, response);
    }

    /**
     * Sends PUT /resources/videos/{id}/views to glimpse-rest and returns
     * the updated view count parsed from the JSON response, or -1 on any error.
     */
    private int callRestPut(int videoId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(REST_VIEWS_BASE + videoId + "/views");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            if (conn.getResponseCode() != 200) return -1;

            // Read response body: {"id":X,"views":Y}
            try (InputStream in = conn.getInputStream()) {
                String body = new String(in.readAllBytes(), "UTF-8");
                return parseViewsFromJson(body);
            }
        } catch (Exception e) {
            // glimpse-rest is offline, fail gracefully
            return -1;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Extracts "views" int from {"id":1,"views":42} without a JSON lib. */
    private int parseViewsFromJson(String json) {
        String key = "\"views\":";
        int idx = json.indexOf(key);
        if (idx < 0) return -1;
        idx += key.length();
        int end = idx;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try {
            return Integer.parseInt(json.substring(idx, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}