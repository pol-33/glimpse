/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
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
        
        String action = request.getParameter("action");
        DBManager dbManager = new DBManager();

        if ("register".equals(action)) {
            // Recoger datos del formulario
            String name = request.getParameter("name");
            String surname = request.getParameter("surname");
            String email = request.getParameter("email");
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            // CONTROL DE ERRORES: Validar contraseñas
            if (!password.equals(confirmPassword)) {
                request.setAttribute("error", "Passwords do not match.");
                request.getRequestDispatcher("registerUser.jsp").forward(request, response);
                return;
            }

            // Intentar registrar en BD
            boolean success = dbManager.registerUser(username, name, surname, email, password);
            if (success) {
                // Redirigir a login con éxito
                request.setAttribute("error", "Registration successful! Please login."); // Usamos "error" para mostrar el mensaje
                request.getRequestDispatcher("login.jsp").forward(request, response);
            } else {
                // Error (probablemente el usuario ya existe)
                request.setAttribute("error", "Error: Username already exists or database error.");
                request.getRequestDispatcher("registerUser.jsp").forward(request, response);
            }

        } else if ("login".equals(action)) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            boolean isValid = dbManager.validateLogin(username, password);

            if (isValid) {
                // INICIO DE SESIÓN CORRECTO
                HttpSession session = request.getSession();
                session.setAttribute("loggedUser", username); // Guardamos la sesión
                
                // Redirigimos al listado de vídeos (que haremos en el siguiente paso)
                response.sendRedirect("listVideos.jsp");
            } else {
                // Error de login
                request.setAttribute("error", "Invalid username or password.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
            }
        }
    }
}