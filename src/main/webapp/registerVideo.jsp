<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Register Video</title>
</head>
<body>
    <%-- Session check: if not logged in, redirect to login --%>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
    %>

    <h2>Register Video</h2>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color:red;"><%= request.getAttribute("error") %></p>
    <% } %>

    <form action="RegisterVideoServlet" method="POST">

        <label>ID:</label>
        <input type="number" name="id" required><br><br>

        <label>Title:</label>
        <input type="text" name="title" required><br><br>

        <label>Author:</label>
        <input type="text" name="author" required><br><br>

        <label>Creation date:</label>
        <input type="date" name="creationDate" required><br><br>

        <label>Duration (HH:mm):</label>
        <input type="time" name="duration" required><br><br>

        <label>Views:</label>
        <input type="number" name="views" min="0" value="0" required><br><br>

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