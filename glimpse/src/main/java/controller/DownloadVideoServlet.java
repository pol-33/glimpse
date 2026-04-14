package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import model.DBManager;
import model.Video;

@WebServlet(name = "DownloadVideoServlet", urlPatterns = {"/DownloadVideoServlet"})
public class DownloadVideoServlet extends HttpServlet {

    private static final String UPLOAD_DIR =
        System.getProperty("user.home") + File.separator + "glimpse-uploads";

    private static final int BUFFER_SIZE = 64 * 1024;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

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

        String storedFilename = video.getFilePath();
        if (storedFilename == null || storedFilename.isBlank()
                || storedFilename.contains("/") || storedFilename.contains("\\")
                || storedFilename.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename.");
            return;
        }

        File file = new File(UPLOAD_DIR, storedFilename);
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String downloadName = video.getOriginalFilename() != null
            ? video.getOriginalFilename()
            : storedFilename;

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        response.setContentType(mimeType);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Disposition",
            "attachment; filename=\"" + downloadName.replace("\"", "") + "\"");

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
