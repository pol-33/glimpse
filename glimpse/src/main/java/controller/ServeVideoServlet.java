package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import model.DBManager;
import model.Video;

/**
 * Serves video files that were uploaded to the local server.
 * Supports HTTP Range requests so the browser can seek inside the video.
 *
 * Security:
 *  - Only logged-in users can access files.
 *  - The 'file' parameter is validated to be a plain filename (no path traversal).
 *
 * URL: GET /ServeVideoServlet?file=<storedFilename>
 *      e.g. /ServeVideoServlet?file=550e8400-e29b-41d4-a716-446655440000.mp4
 */
@WebServlet(name = "ServeVideoServlet", urlPatterns = {"/ServeVideoServlet"})
public class ServeVideoServlet extends HttpServlet {

    /** Must match the UPLOAD_DIR constant in RegisterVideoServlet. */
    private static final String UPLOAD_DIR =
        System.getProperty("user.home") + File.separator + "glimpse-uploads";

    private static final int BUFFER_SIZE = 64 * 1024; // 64 KB chunks

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Auth
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid video id.");
            return;
        }

        Video video = new DBManager().getVideoById(id);
        if (video == null || !"upload".equals(video.getFileSource())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filename = video.getFilePath();
        if (filename == null || filename.isBlank()
                || filename.contains("/") || filename.contains("\\")
                || filename.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename.");
            return;
        }

        File file = new File(UPLOAD_DIR, filename);
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileLength = file.length();

        // Content-Type
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";
        response.setContentType(mimeType);
        response.setHeader("Accept-Ranges", "bytes");

        // Range request support (needed for browser video seeking)
        String rangeHeader = request.getHeader("Range");

        long start = 0;
        long end   = fileLength - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] parts = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(parts[0].trim());
                end   = (parts.length > 1 && !parts[1].trim().isEmpty())
                        ? Long.parseLong(parts[1].trim())
                        : fileLength - 1;
            } catch (NumberFormatException ignored) {
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            if (start > end || end >= fileLength) {
                response.setHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range",
                "bytes " + start + "-" + end + "/" + fileLength);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        long contentLength = end - start + 1;
        response.setHeader("Content-Length", String.valueOf(contentLength));

        // Stream the bytes
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {

            raf.seek(start);
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = contentLength;

            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }
}
