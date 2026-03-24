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

    private static final int PAGE_SIZE = 20;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String loggedUser = (String) session.getAttribute("loggedUser");

        // Consume flash message from session (set by DeleteVideoServlet on error)
        String error = (String) session.getAttribute("error");
        if (error != null) {
            request.setAttribute("error", error);
            session.removeAttribute("error"); // remove immediately so it shows only once
        }

        // Parse page parameter — default to 0
        int page = 0;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null) page = Math.max(0, Integer.parseInt(pageParam));
        } catch (NumberFormatException ignored) {}

        DBManager dbManager = new DBManager();

        int totalVideos = dbManager.getVideoCount();
        int totalPages  = (int) Math.ceil((double) totalVideos / PAGE_SIZE);

        // Clamp page to valid range
        page = Math.min(page, Math.max(0, totalPages - 1));

        List<Video> videos = dbManager.getVideosPage(page, PAGE_SIZE, loggedUser);

        request.setAttribute("videos",      videos);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages",  totalPages);
        request.setAttribute("totalVideos", totalVideos);

        request.getRequestDispatcher("listVideos.jsp").forward(request, response);
    }
}