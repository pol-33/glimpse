package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import model.DBManager;
import model.Video;

@WebServlet(name = "ListVideosServlet", urlPatterns = {"/ListVideosServlet"})
public class ListVideosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Session check
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        // Fetch all videos from DB and pass them to the JSP
        DBManager dbManager = new DBManager();
        List<Video> videos = dbManager.getAllVideos();

        if (videos == null || videos.isEmpty()) {
            request.setAttribute("info", "No videos registered yet.");
        }

        request.setAttribute("videos", videos);
        request.getRequestDispatcher("listVideos.jsp").forward(request, response);
    }
}