package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import model.DBManager;
import model.Video;

@WebServlet(name = "RegisterVideoServlet", urlPatterns = {"/RegisterVideoServlet"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize       = 1024 * 1024 * 500,
    maxRequestSize    = 1024 * 1024 * 510
)
public class RegisterVideoServlet extends HttpServlet {

    private static final String UPLOAD_DIR =
        System.getProperty("user.home") + File.separator + "glimpse-uploads";

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
        String fileChoice  = request.getParameter("fileChoice"); // "url" or "upload" only

        // Auto-set values
        String author          = (String) session.getAttribute("loggedUser");
        LocalDate creationDate = LocalDate.now();
        int views              = 0;

        // Null-safe empty field check — format excluded for uploads
        // because the server derives it from the filename
        if (title == null || title.trim().isEmpty()
                || durationStr == null || durationStr.trim().isEmpty()) {
            request.setAttribute("error", "All required fields must be filled in.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Enforce file source is present and valid
        if (fileChoice == null
                || (!fileChoice.equals("url") && !fileChoice.equals("upload"))) {
            request.setAttribute("error", "You must provide either a URL or upload a file.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Parse and validate duration
        LocalTime duration;
        try {
            if (durationStr.length() == 5) durationStr += ":00";
            duration = LocalTime.parse(durationStr);
        } catch (DateTimeParseException e) {
            request.setAttribute("error", "Invalid duration format.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Reject 00:00:00 (a video with zero duration is invalid)
        if (duration.equals(LocalTime.MIDNIGHT)) {
            request.setAttribute("error", "Duration cannot be 00:00:00.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        // Resolve file path, original filename, and format
        String filePath         = null;
        String originalFilename = null;

        if ("url".equals(fileChoice)) {
            String rawUrl = request.getParameter("fileUrl");
            if (rawUrl == null || rawUrl.trim().isEmpty()) {
                request.setAttribute("error", "Please provide a valid URL.");
                request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
                return;
            }
            filePath = rawUrl.trim();

            // For URLs, format comes from the form, validate it is not empty
            if (format == null || format.trim().isEmpty()) {
                request.setAttribute("error", "Please specify the video format.");
                request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
                return;
            }
            format = format.trim().toLowerCase();

        } else { // "upload"
            Part filePart = request.getPart("fileUpload");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("error", "Please select a file to upload.");
                request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
                return;
            }

            originalFilename = Paths.get(
                filePart.getSubmittedFileName()).getFileName().toString();

            // Derive and validate format server-side, never trust the client-submitted value
            String ext = extractExtension(originalFilename);
            if (ext == null) {
                request.setAttribute("error",
                    "Could not determine format. " +
                    "Please rename your file with a valid extension (e.g. video.mp4).");
                request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
                return;
            }
            format = ext;

            String storedName = UUID.randomUUID().toString() + "." + ext;

            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            Path destination = Paths.get(UPLOAD_DIR, storedName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            filePath = storedName;
        }

        DBManager dbManager = new DBManager();

        // Check for duplicate title
        if (dbManager.videoExists(title.trim())) {
            request.setAttribute("error", "A video with that title already exists.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
            return;
        }

        Video video = new Video(title.trim(), author, creationDate, duration,
                                views, description, format,
                                filePath, originalFilename, fileChoice);

        boolean success = dbManager.registerVideo(video);
        if (success) {
            response.sendRedirect("ListVideosServlet");
        } else {
            request.setAttribute("error", "Database error. Please try again.");
            request.getRequestDispatcher("registerVideo.jsp").forward(request, response);
        }
    }

    /**
     * Extracts and validates the file extension from a filename.
     * Returns the lowercase extension (without the dot) if valid,
     * or null if the extension is missing, empty, or contains
     * non-alphanumeric characters.
     *
     * Examples:
     *   "video.mp4"       → "mp4"
     *   "hola.video.mp4"  → "mp4"
     *   "video."          → null  (empty extension)
     *   "video"           → null  (no dot)
     *   "video.mp 4"      → null  (space in extension)
     */
    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return null;                    // no dot at all
        String ext = filename.substring(dot + 1).toLowerCase();
        if (ext.isEmpty()) return null;              // trailing dot, e.g. "video."
        if (!ext.matches("[a-z0-9]+")) return null;  // only allow alphanumeric extensions
        return ext;
    }
}