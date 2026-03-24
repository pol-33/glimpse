package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.DBManager;

@WebServlet(name = "LikeVideoServlet", urlPatterns = {"/LikeVideoServlet"})
public class LikeVideoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // Session check
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String loggedUser = (String) session.getAttribute("loggedUser");
        String page       = request.getParameter("page");
        String redirect   = "ListVideosServlet?page=" + (page != null ? page : "0");

        // Parse video ID
        int videoId;
        try {
            videoId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendRedirect(redirect);
            return;
        }

        DBManager dbManager = new DBManager();

        // Toggle like: if already liked, unlike; otherwise like
        if (dbManager.hasLiked(videoId, loggedUser)) {
            dbManager.unlikeVideo(videoId, loggedUser);
        } else {
            dbManager.likeVideo(videoId, loggedUser);
        }

        response.sendRedirect(redirect);
    }
}