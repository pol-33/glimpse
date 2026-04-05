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

            <%-- Shared table fragment --%>
            <%@include file="_videoTable.jsp"%>

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
