<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
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

    <% if (request.getAttribute("error") != null) { %>
        <p style="color:red;"><%= request.getAttribute("error") %></p>
    <% } %>

    <%
        List<Video> videos       = (List<Video>) request.getAttribute("videos");
        Map<Integer, Integer> likes = (Map<Integer, Integer>) request.getAttribute("likes");
        Set<Integer> likedByUser    = (Set<Integer>) request.getAttribute("likedByUser");

        if (videos != null && !videos.isEmpty()) {
            String loggedUser = (String) session.getAttribute("loggedUser");
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
            <th>Likes</th>
            <th>Actions</th>
        </tr>
        <%
            for (Video v : videos) {
                int likeCount = likes.getOrDefault(v.getId(), 0);
                boolean userHasLiked = likedByUser.contains(v.getId());
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
            <td><%= likeCount %></td>
            <td>
                <%-- Like / Unlike button --%>
                <form action="LikeVideoServlet" method="POST" style="display:inline;">
                    <input type="hidden" name="id" value="<%= v.getId() %>">
                    <input type="submit" value="<%= userHasLiked ? "Unlike" : "Like" %>">
                </form>

                <%-- Delete button (only for own videos) --%>
                <% if (loggedUser.equals(v.getAuthor())) { %>
                    <form action="DeleteVideoServlet" method="POST" style="display:inline;"
                          onsubmit="return confirm('Delete \'<%= v.getTitle() %>\'?');">
                        <input type="hidden" name="id" value="<%= v.getId() %>">
                        <input type="submit" value="Delete">
                    </form>
                <% } %>
            </td>
        </tr>
        <%
            }
        %>
    </table>
    <% } else { %>
        <p style="color:blue;">No videos registered yet.</p>
    <% } %>
</body>
</html>