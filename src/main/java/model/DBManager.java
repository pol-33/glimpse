/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBManager {
    
    // Método privado para conectar a la base de datos
    private Connection getConnection() throws Exception {
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        return DriverManager.getConnection("jdbc:derby://localhost:1527/pr2", "pr2", "pr2");
    }

    // Método para registrar un usuario
    public boolean registerUser(String username, String name, String surname, String email, String password) {
        String query = "INSERT INTO users (username, name, surname, email, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setString(3, surname);
            pstmt.setString(4, email);
            pstmt.setString(5, password);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Devuelve true si se insertó correctamente
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Devuelve false si hubo error (ej. username ya existe)
        }
    }

    // Método para validar el login
    public boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Devuelve true si encuentra al usuario
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}