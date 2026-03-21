package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import model.DBManager;
import model.Video;

@WebServlet(name = "RegisterVideoServlet", urlPatterns = {"/RegisterVideoServlet"})
public class RegisterVideoServlet extends HttpServlet {

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

        // Retrieve form parameters
        String title       = request.getParameter("title");
        String durationStr = request.getParameter("duration");
        String description = request.getParameter("description");
        String format      = request.getParameter("format");
        String filePath    = request.getParameter("filePath");

        // Auto-set values
        String author          = (String) session.getAttribute("loggedUser");
        LocalDate creationDate = LocalDate.now();
        int views              = 0;

        // Control empty fields
        if (title.isEmpty() || durationStr.isEmpty() || format.isEmpty()) {
            request.setAttribute("error", "All required fields must be filled in.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Parse and validate duration
        LocalTime duration;
        try {
            duration = LocalTime.parse(durationStr);
        } catch (DateTimeParseException e) {
            request.setAttribute("error", "Invalid duration format.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        DBManager dbManager = new DBManager();

        // Check for duplicate title
        if (dbManager.videoExists(title)) {
            request.setAttribute("error", "A video with that title already exists.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Build Video object and save it
        Video video = new Video(title, author, creationDate, duration,
                                views, description, format, filePath);

        boolean success = dbManager.registerVideo(video);
        if (success) {
            response.sendRedirect("ListVideosServlet");
        } else {
            request.setAttribute("error", "Database error. Please try again.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
        }
    }
}