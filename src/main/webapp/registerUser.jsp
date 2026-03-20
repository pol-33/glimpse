<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Register - Video System</title>
</head>
<body>
    <h2>Register User</h2>
    <!-- Mostrar errores si los hay -->
    <% if(request.getAttribute("error") != null) { %>
        <p style="color:red;"><%= request.getAttribute("error") %></p>
    <% } %>

    <form action="UserServlet" method="POST">
        <input type="hidden" name="action" value="register">
        
        <label>Name:</label>
        <input type="text" name="name" required><br><br>
        
        <label>Surname:</label>
        <input type="text" name="surname" required><br><br>
        
        <label>Email:</label>
        <input type="email" name="email" required><br><br>
        
        <label>Username:</label>
        <input type="text" name="username" required><br><br>
        
        <label>Password:</label>
        <input type="password" name="password" required><br><br>
        
        <label>Repeat password:</label>
        <input type="password" name="confirmPassword" required><br><br>
        
        <input type="submit" value="Register User">
    </form>
    <p>Already registered? <a href="login.jsp">Go to Login</a></p>
</body>
</html>