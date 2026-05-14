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
import java.util.Set;

@WebServlet(name = "DownloadXmlServlet", urlPatterns = {"/DownloadXmlServlet"})
public class DownloadXmlServlet extends HttpServlet {

    private static final String XML_DIR =
        System.getProperty("user.home") + File.separator + "glimpse-xml";

    /** Whitelist of downloadable filenames — prevents path traversal. */
    private static final Set<String> ALLOWED = Set.of(
        "didlFilm1.xml",
        "didlFilm1_encrypted.xml",
        "didlFilm1_decrypted.xml"
    );

    private static final int BUFFER_SIZE = 32 * 1024;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String name = request.getParameter("file");
        if (name == null || !ALLOWED.contains(name)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename.");
            return;
        }

        File file = new File(XML_DIR, name);
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("application/xml");
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");

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
