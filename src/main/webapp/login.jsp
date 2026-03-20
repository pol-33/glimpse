<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Login - Video System</title>
</head>
<body>
    <h2>Login</h2>
    <!-- Mostrar errores si los hay -->
    <% if(request.getAttribute("error") != null) { %>
        <p style="color:red;"><%= request.getAttribute("error") %></p>
    <% } %>

    <form action="UserServlet" method="POST">
        <input type="hidden" name="action" value="login">
        
        <label>Username:</label>
        <input type="text" name="username" required><br><br>
        
        <label>Password:</label>
        <input type="password" name="password" required><br><br>
        
        <input type="submit" value="Login">
    </form>
    <p>Don't have an account? <a href="registerUser.jsp">Register here</a></p>
</body>
</html>