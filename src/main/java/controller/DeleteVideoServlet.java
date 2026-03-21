package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.DBManager;

@WebServlet(name = "DeleteVideoServlet", urlPatterns = {"/DeleteVideoServlet"})
public class DeleteVideoServlet extends HttpServlet {

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

        // Parse and validate video ID
        String idStr = request.getParameter("id");
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            response.sendRedirect("ListVideosServlet");
            return;
        }

        DBManager dbManager = new DBManager();

        // deleteVideo checks both id AND author, so a user cannot
        // delete another user's video even via a direct POST request
        boolean success = dbManager.deleteVideo(id, loggedUser);
        if (!success) {
            // Either the video doesn't exist or it belongs to someone else
            request.setAttribute("error", "You can only delete your own videos.");
            request.getRequestDispatcher("ListVideosServlet").forward(request, response);
            return;
        }

        response.sendRedirect("ListVideosServlet");
    }
}