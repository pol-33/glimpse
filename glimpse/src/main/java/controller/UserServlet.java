package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.DBManager;
import security.Csrf;

@WebServlet(name = "UserServlet", urlPatterns = {"/UserServlet"})
public class UserServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        HttpSession session = request.getSession(false);
        if (!Csrf.isValid(request, session)) {
            request.setAttribute("error", "Security token missing or expired. Please try again.");
            request.getRequestDispatcher("register".equals(action) ? "registerUser.jsp" : "login.jsp")
                   .forward(request, response);
            return;
        }

        DBManager dbManager = new DBManager();

        if ("register".equals(action)) {
            String name            = request.getParameter("name");
            String surname         = request.getParameter("surname");
            String email           = request.getParameter("email");
            String username        = request.getParameter("username");
            String password        = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            // Null-safe empty field check
            if (name == null || name.trim().isEmpty()
                    || surname == null || surname.trim().isEmpty()
                    || email == null || email.trim().isEmpty()
                    || username == null || username.trim().isEmpty()
                    || password == null || password.trim().isEmpty()) {
                request.setAttribute("error", "All fields must be filled in.");
                request.getRequestDispatcher("registerUser.jsp").forward(request, response);
                return;
            }

            // Validate passwords match
            if (!password.equals(confirmPassword)) {
                request.setAttribute("error", "Passwords do not match.");
                request.getRequestDispatcher("registerUser.jsp").forward(request, response);
                return;
            }

            // Check for duplicate username
            if (dbManager.userExists(username)) {
                request.setAttribute("error", "Username '" + username + "' is already taken.");
                request.getRequestDispatcher("registerUser.jsp").forward(request, response);
                return;
            }

            boolean success = dbManager.registerUser(
                username.trim(), name.trim(), surname.trim(), email.trim(), password);
            if (success) {
                request.setAttribute("info", "Registration successful! Please login.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
            } else {
                request.setAttribute("error", "Database error. Please try again.");
                request.getRequestDispatcher("registerUser.jsp").forward(request, response);
            }

        } else if ("login".equals(action)) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            // Null-safe empty field check
            if (username == null || username.trim().isEmpty()
                    || password == null || password.trim().isEmpty()) {
                request.setAttribute("error", "Username and password must be filled in.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
                return;
            }

            boolean isValid = dbManager.validateLogin(username.trim(), password);
            if (isValid) {
                if (session != null) {
                    session.invalidate();
                }
                HttpSession newSession = request.getSession(true);
                newSession.setAttribute("loggedUser", username.trim());
                Csrf.rotateToken(newSession);
                response.sendRedirect("ListVideosServlet");
            } else {
                request.setAttribute("error", "Invalid username or password.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
            }
        }
    }
}
