<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Register Video</title>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
    %>

    <h2>Register Video</h2>
    <p>Publishing as: <strong><%= session.getAttribute("loggedUser") %></strong></p>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color:red;"><%= request.getAttribute("error") %></p>
    <% } %>

    <form action="RegisterVideoServlet" method="POST">

        <label>Title:</label>
        <input type="text" name="title" required><br><br>

        <label>Duration (HH:mm:ss):</label>
        <input type="time" name="duration" step="1" required><br><br>

        <label>Description:</label>
        <input type="text" name="description"><br><br>

        <label>Format:</label>
        <input type="text" name="format" required
               placeholder="e.g. mp4, ogg"><br><br>

        <label>File path / URL:</label>
        <input type="text" name="filePath"
               placeholder="e.g. http://example.com/video.mp4"><br><br>

        <input type="submit" value="Register Video">
    </form>

    <p><a href="ListVideosServlet">Back to video list</a></p>
</body>
</html>