package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.DBManager;
import security.Csrf;

/**
 * Handles like/unlike via AJAX (fetch API).
 *
 * Method : POST
 * Input  : form param "id" (video id)
 * Output : JSON  {"liked": true|false, "likeCount": <int>}
 *
 * Used by listVideos.jsp, searchResults.jsp and playVideo.jsp so that
 * likes can be toggled without reloading the page (and restarting the video).
 */
@WebServlet(name = "LikeAjaxServlet", urlPatterns = {"/LikeAjaxServlet"})
public class LikeAjaxServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Auth
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not logged in\"}");
            return;
        }
        if (!Csrf.isValid(request, session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Invalid CSRF token\"}");
            return;
        }

        String loggedUser = (String) session.getAttribute("loggedUser");

        // Parse video id
        int videoId;
        try {
            videoId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid id\"}");
            return;
        }

        // Toggle like and return new state
        DBManager db = new DBManager();
        int[] result = db.toggleLike(videoId, loggedUser);
        // result[0] = 1 if now liked, 0 if now unliked
        // result[1] = new total like count

        response.getWriter().write(
            "{\"liked\":" + (result[0] == 1 ? "true" : "false") +
            ",\"likeCount\":" + result[1] + "}"
        );
    }
}
