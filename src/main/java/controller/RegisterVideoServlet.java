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

        // Session check: if not logged in, redirect to login
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        // Retrieve form parameters
        String idStr        = request.getParameter("id");
        String title        = request.getParameter("title");
        String author       = request.getParameter("author");
        String dateStr      = request.getParameter("creationDate");
        String durationStr  = request.getParameter("duration");
        String viewsStr     = request.getParameter("views");
        String description  = request.getParameter("description");
        String format       = request.getParameter("format");
        String filePath     = request.getParameter("filePath");

        // Control empty fields
        if (idStr.isEmpty() || title.isEmpty() || author.isEmpty()
                || dateStr.isEmpty() || durationStr.isEmpty() || format.isEmpty()) {
            request.setAttribute("error", "All required fields must be filled in.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Parse and validate ID
        int id;
        try {
            id = Integer.parseInt(idStr);
            if (id < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            request.setAttribute("error", "ID must be a positive integer.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Parse and validate date
        LocalDate creationDate;
        try {
            creationDate = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            request.setAttribute("error", "Invalid date format.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Parse and validate duration
        // HTML time inputs give "HH:mm", LocalTime.parse needs "HH:mm:ss"
        LocalTime duration;
        try {
            if (durationStr.length() == 5) durationStr += ":00";
            duration = LocalTime.parse(durationStr);
        } catch (DateTimeParseException e) {
            request.setAttribute("error", "Invalid duration format.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Parse and validate views
        int views;
        try {
            views = Integer.parseInt(viewsStr);
            if (views < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            request.setAttribute("error", "Views must be a non-negative integer.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        DBManager dbManager = new DBManager();

        // Check for duplicate video ID
        if (dbManager.videoExists(id)) {
            request.setAttribute("error", "A video with ID " + id + " already exists.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Build the Video object and save it
        Video video = new Video(id, title, author, creationDate, duration,
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