<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="model.Video"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse - Videos</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        List<Video> videos          = (List<Video>) request.getAttribute("videos");
        Map<Integer, Integer> likes = (Map<Integer, Integer>) request.getAttribute("likes");
        Set<Integer> likedByUser    = (Set<Integer>) request.getAttribute("likedByUser");
        String loggedUser           = (String) session.getAttribute("loggedUser");
    %>

    <div class="container py-5">

        <div class="d-flex justify-content-between align-items-start mb-4">
            <div>
                <h1 class="page-title">Videos</h1>
                <p class="page-subtitle" style="margin-bottom:0;">
                    Browse everything published on Glimpse.
                </p>
            </div>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert-glimpse-error mb-4">
                <i class="bi bi-exclamation-circle me-2"></i><%= request.getAttribute("error") %>
            </div>
        <% } %>

        <% if (videos != null && !videos.isEmpty()) { %>
        <div style="overflow-x: auto;">
            <table class="glimpse-table">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Title</th>
                        <th>Author</th>
                        <th>Date</th>
                        <th>Duration</th>
                        <th>Views</th>
                        <th>Format</th>
                        <th>Description</th>
                        <th>File</th>
                        <th>Likes</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                <%
                    for (Video v : videos) {
                        int likeCount = likes.getOrDefault(v.getId(), 0);
                        boolean userHasLiked = likedByUser.contains(v.getId());
                %>
                    <tr>
                        <td style="color:var(--glimpse-muted); font-size:0.8rem;">
                            <%= v.getId() %>
                        </td>
                        <td style="font-weight:600; color:var(--glimpse-dark);">
                            <%= v.getTitle() %>
                        </td>
                        <td>
                            <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                                         padding:2px 10px; border-radius:20px; font-size:0.82rem;
                                         font-weight:600;">
                                <%= v.getAuthor() %>
                            </span>
                        </td>
                        <td style="color:var(--glimpse-muted); font-size:0.88rem;">
                            <%= v.getCreationDate() %>
                        </td>
                        <td style="font-size:0.88rem;"><%= v.getDuration() %></td>
                        <td style="font-size:0.88rem;"><%= v.getViews() %></td>
                        <td>
                            <span style="background:#F1F5F9; color:var(--glimpse-muted);
                                         padding:2px 8px; border-radius:4px; font-size:0.8rem;
                                         font-family:monospace;">
                                <%= v.getFormat() %>
                            </span>
                        </td>
                        <td style="font-size:0.88rem; color:var(--glimpse-muted); max-width:180px;">
                            <%= v.getDescription() != null ? v.getDescription() : "" %>
                        </td>
                        <td>
                            <% if (v.getFilePath() != null && v.getFilePath().startsWith("http")) { %>
                                <a href="<%= v.getFilePath() %>" target="_blank"
                                   style="color:var(--glimpse-accent); font-size:0.88rem; font-weight:600;">
                                    <i class="bi bi-box-arrow-up-right me-1"></i>Open
                                </a>
                            <% } else { %>
                                <span style="color:var(--glimpse-muted); font-size:0.85rem;">
                                    <%= v.getFilePath() != null ? v.getFilePath() : "-" %>
                                </span>
                            <% } %>
                        </td>
                        <td>
                            <span class="like-count">
                                <i class="bi bi-heart-fill" style="font-size:0.85rem;"></i>
                                <%= likeCount %>
                            </span>
                        </td>
                        <td class="actions-cell">
                            <div class="d-flex gap-2 flex-wrap">
                                <%-- Like / Unlike --%>
                                <form action="LikeVideoServlet" method="POST">
                                    <input type="hidden" name="id" value="<%= v.getId() %>">
                                    <button type="submit"
                                            class="btn-glimpse-like <%= userHasLiked ? "active" : "" %>">
                                        <i class="bi bi-heart<%= userHasLiked ? "-fill" : "" %> me-1"></i>
                                        <%= userHasLiked ? "Unlike" : "Like" %>
                                    </button>
                                </form>

                                <%-- Delete (own videos only) --%>
                                <% if (loggedUser.equals(v.getAuthor())) { %>
                                    <form action="DeleteVideoServlet" method="POST"
                                          onsubmit="return confirm('Delete \'<%= v.getTitle() %>\'?');">
                                        <input type="hidden" name="id" value="<%= v.getId() %>">
                                        <button type="submit" class="btn-glimpse-danger">
                                            <i class="bi bi-trash me-1"></i>Delete
                                        </button>
                                    </form>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                <%
                    }
                %>
                </tbody>
            </table>
        </div>
        <% } else { %>
            <div class="glimpse-card text-center py-5">
                <i class="bi bi-camera-video" style="font-size:3rem; color:#C7D2FE;"></i>
                <p style="color:var(--glimpse-muted); margin-top:1rem; font-size:1.05rem;">
                    No videos yet. Be the first to publish!
                </p>
                <a href="registerVideo.jsp" class="btn-glimpse mt-2">
                    <i class="bi bi-plus-lg me-1"></i>New Video
                </a>
            </div>
        <% } %>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>