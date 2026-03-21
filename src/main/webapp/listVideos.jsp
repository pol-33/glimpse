<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="model.Video"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Video List</title>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
    %>

    <h2>Video List</h2>
    <p>Welcome, <strong><%= session.getAttribute("loggedUser") %></strong>!
       <a href="logout.jsp">Logout</a> |
       <a href="registerVideo.jsp">Register new video</a>
    </p>

    <% if (request.getAttribute("info") != null) { %>
        <p style="color:blue;"><%= request.getAttribute("info") %></p>
    <% } %>

    <%
        List<Video> videos = (List<Video>) request.getAttribute("videos");
        if (videos != null && !videos.isEmpty()) {
    %>
    <table border="1" cellpadding="8">
        <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Author</th>
            <th>Creation Date</th>
            <th>Duration</th>
            <th>Views</th>
            <th>Description</th>
            <th>Format</th>
            <th>File Path</th>
        </tr>
        <%
            for (Video v : videos) {
        %>
        <tr>
            <td><%= v.getId() %></td>
            <td><%= v.getTitle() %></td>
            <td><%= v.getAuthor() %></td>
            <td><%= v.getCreationDate() %></td>
            <td><%= v.getDuration() %></td>
            <td><%= v.getViews() %></td>
            <td><%= v.getDescription() %></td>
            <td><%= v.getFormat() %></td>
            <td>
                <% if (v.getFilePath() != null && v.getFilePath().startsWith("http")) { %>
                    <a href="<%= v.getFilePath() %>" target="_blank">Open link</a>
                <% } else { %>
                    <%= v.getFilePath() %>
                <% } %>
            </td>
        </tr>
        <%
            }
        %>
    </table>
    <% } %>
</body>
</html>