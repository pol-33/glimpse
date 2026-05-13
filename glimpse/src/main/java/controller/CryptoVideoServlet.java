package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import model.DBManager;
import model.Video;
import security.ContentCrypto;
import security.Csrf;

@WebServlet(name = "CryptoVideoServlet", urlPatterns = {"/CryptoVideoServlet"})
public class CryptoVideoServlet extends HttpServlet {

    private static final String PASSPHRASE = "glimpse-video-aes-key-2026-fib-upc"; // To be changed before any real deployment.
    private static final Path UPLOAD_DIR =
        Paths.get(System.getProperty("user.home") + File.separator + "glimpse-uploads")
             .toAbsolutePath().normalize();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        if (!Csrf.isValid(request, session)) {
            session.setAttribute("error", "Security token missing or expired.");
            response.sendRedirect("ListVideosServlet");
            return;
        }

        String loggedUser = (String) session.getAttribute("loggedUser");
        String redirect = "ListVideosServlet";

        String idStr  = request.getParameter("videoId");
        String action = request.getParameter("action");

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            session.setAttribute("error", "Invalid video ID.");
            response.sendRedirect(redirect);
            return;
        }

        if (!"encrypt".equals(action) && !"decrypt".equals(action)) {
            session.setAttribute("error", "Invalid action.");
            response.sendRedirect(redirect);
            return;
        }

        DBManager db = new DBManager();
        Video video = db.getVideoById(id);

        if (video == null) {
            session.setAttribute("error", "Video not found.");
            response.sendRedirect(redirect);
            return;
        }
        if (!video.getAuthor().equals(loggedUser)) {
            session.setAttribute("error", "You can only encrypt/decrypt your own videos.");
            response.sendRedirect(redirect);
            return;
        }
        if (!"upload".equals(video.getFileSource())) {
            session.setAttribute("error", "Only locally uploaded files can be encrypted.");
            response.sendRedirect(redirect);
            return;
        }

        try {
            SecretKey key = deriveKey(PASSPHRASE);
            String storedName = video.getFilePath();
            Path src = UPLOAD_DIR.resolve(storedName).normalize();
            if (!src.startsWith(UPLOAD_DIR)) {
                throw new SecurityException("Path traversal detected.");
            }

            if ("encrypt".equals(action)) {
                String encName = storedName + ".enc";
                Path dst = UPLOAD_DIR.resolve(encName).normalize();
                ContentCrypto.encryptFile(src, dst, key);
                Files.delete(src);
                db.updateFilePath(id, encName);
                session.setAttribute("success", "Video encrypted successfully.");
            } else {
                if (!storedName.endsWith(".enc")) {
                    session.setAttribute("error", "File does not appear to be encrypted.");
                    response.sendRedirect(redirect);
                    return;
                }
                String plainName = storedName.substring(0, storedName.length() - 4);
                Path dst = UPLOAD_DIR.resolve(plainName).normalize();
                ContentCrypto.decryptFile(src, dst, key);
                Files.delete(src);
                db.updateFilePath(id, plainName);
                session.setAttribute("success", "Video decrypted successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("error", "Crypto operation failed: " + e.getMessage());
        }

        response.sendRedirect(redirect);
    }

    private SecretKey deriveKey(String passphrase) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(passphrase.getBytes("UTF-8"));
        byte[] keyBytes = new byte[16];
        System.arraycopy(hash, 0, keyBytes, 0, 16);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
