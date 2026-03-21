package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.DBManager;

@WebServlet(name = "UserServlet", urlPatterns = {"/UserServlet"})
public class UserServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        DBManager dbManager = new DBManager();

        if ("register".equals(action)) {
            String name            = request.getParameter("name");
            String surname         = request.getParameter("surname");
            String email           = request.getParameter("email");
            String username        = request.getParameter("username");
            String password        = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            // Control empty fields
            if (name.isEmpty() || surname.isEmpty() || email.isEmpty()
                    || username.isEmpty() || password.isEmpty()) {
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

            boolean success = dbManager.registerUser(username, name, surname, email, password);
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

            // Control empty fields
            if (username.isEmpty() || password.isEmpty()) {
                request.setAttribute("error", "Username and password must be filled in.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
                return;
            }

            boolean isValid = dbManager.validateLogin(username, password);
            if (isValid) {
                HttpSession session = request.getSession();
                session.setAttribute("loggedUser", username);
                response.sendRedirect("ListVideosServlet");
            } else {
                request.setAttribute("error", "Invalid username or password.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
            }
        }
    }
}