<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
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
        List<Video> videos  = (List<Video>) request.getAttribute("videos");
        String loggedUser   = (String) session.getAttribute("loggedUser");
        int currentPage     = (Integer) request.getAttribute("currentPage");
        int totalPages      = (Integer) request.getAttribute("totalPages");
        int totalVideos     = (Integer) request.getAttribute("totalVideos");
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
                        boolean canPlay = "upload".equals(v.getFileSource());
                %>
                    <tr>
                        <td style="color:var(--glimpse-muted); font-size:0.8rem;">
                            <%= v.getId() %>
                        </td>
                        <td style="font-weight:600; color:var(--glimpse-dark);">
                            <%= v.getTitle() %>
                        </td>
                        <td>
                            <span style="background:var(--glimpse-light);
                                         color:var(--glimpse-primary);
                                         padding:2px 10px; border-radius:20px;
                                         font-size:0.82rem; font-weight:600;">
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
                                         padding:2px 8px; border-radius:4px;
                                         font-size:0.8rem; font-family:monospace;">
                                <%= v.getFormat() %>
                            </span>
                        </td>
                        <td style="font-size:0.88rem; color:var(--glimpse-muted);
                                   max-width:180px;">
                            <%= v.getDescription() != null ? v.getDescription() : "" %>
                        </td>
                        <td>
                            <% if ("url".equals(v.getFileSource())) { %>
                                <a href="<%= v.getFilePath() %>" target="_blank"
                                   style="color:var(--glimpse-accent); font-size:0.88rem;
                                          font-weight:600;">
                                    <i class="bi bi-box-arrow-up-right me-1"></i>Open link
                                </a>
                            <% } else { %>
                                <span style="color:var(--glimpse-muted); font-size:0.85rem;">
                                    <i class="bi bi-file-earmark-play me-1"></i>
                                    <%= v.getOriginalFilename() %>
                                </span>
                            <% } %>
                        </td>
                        <td>
                            <span class="like-count">
                                <i class="bi bi-heart-fill" style="font-size:0.85rem;"></i>
                                <%= v.getLikeCount() %>
                            </span>
                        </td>
                        <td class="actions-cell">
                            <div class="d-flex gap-2 flex-wrap">

                                <%-- Play (uploaded files only, navigates to dedicated player page) --%>
                                <% if (canPlay) { %>
                                    <a href="PlayVideoServlet?id=<%= v.getId() %>"
                                       class="btn-glimpse-play"
                                       style="text-decoration:none; display:inline-block;">
                                        <i class="bi bi-play-circle me-1"></i>Play
                                    </a>
                                <% } %>

                                <%-- Like / Unlike --%>
                                <form action="LikeVideoServlet" method="POST">
                                    <input type="hidden" name="id"   value="<%= v.getId() %>">
                                    <input type="hidden" name="page" value="<%= currentPage %>">
                                    <button type="submit"
                                            class="btn-glimpse-like <%= v.isUserLiked() ? "active" : "" %>">
                                        <i class="bi bi-heart<%= v.isUserLiked() ? "-fill" : "" %> me-1"></i>
                                        <%= v.isUserLiked() ? "Unlike" : "Like" %>
                                    </button>
                                </form>

                                <%-- Delete (own videos only) --%>
                                <% if (loggedUser.equals(v.getAuthor())) { %>
                                    <form action="DeleteVideoServlet" method="POST"
                                          onsubmit="return confirm('Delete this video?');">
                                        <input type="hidden" name="id"   value="<%= v.getId() %>">
                                        <input type="hidden" name="page" value="<%= currentPage %>">
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

        <%-- Pagination --%>
        <% if (totalPages > 0) { %>
        <div class="d-flex align-items-center justify-content-between mt-4">
            <span style="color:var(--glimpse-muted); font-size:0.88rem;">
                Page <%= currentPage + 1 %> of <%= totalPages %>
                (<%= totalVideos %> videos total)
            </span>
            <div class="d-flex gap-2">
                <% if (currentPage > 0) { %>
                    <a href="ListVideosServlet?page=<%= currentPage - 1 %>"
                       class="btn-glimpse-outline" style="padding:0.4rem 1rem;">
                        <i class="bi bi-chevron-left me-1"></i>Previous
                    </a>
                <% } %>
                <% if (currentPage < totalPages - 1) { %>
                    <a href="ListVideosServlet?page=<%= currentPage + 1 %>"
                       class="btn-glimpse" style="padding:0.4rem 1rem;">
                        Next<i class="bi bi-chevron-right ms-1"></i>
                    </a>
                <% } %>
            </div>
        </div>
        <% } %>

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
