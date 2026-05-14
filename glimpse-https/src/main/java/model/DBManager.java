package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import security.PasswordSecurity;

public class DBManager {

    private static final String URL  = "jdbc:derby://localhost:1527/pr2";
    private static final String USER = "pr2";
    private static final String PASS = "pr2";

    private Connection getConnection() throws Exception {
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public boolean userExists(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            return pstmt.executeQuery().next();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean registerUser(String username, String name, String surname,
                                String email, String password) {
        String query = "INSERT INTO users (username, name, surname, email, password) "
                     + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setString(3, surname);
            pstmt.setString(4, email);
            pstmt.setString(5, PasswordSecurity.hashPassword(password));
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}
