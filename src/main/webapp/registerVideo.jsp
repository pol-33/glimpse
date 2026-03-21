<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse — New Video</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
    %>

    <div class="container py-5">
        <div style="max-width: 620px; margin: 0 auto;">

            <h1 class="page-title">New Video</h1>
            <p class="page-subtitle">
                Publishing as <strong><%= session.getAttribute("loggedUser") %></strong>
            </p>

            <div class="glimpse-card">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-glimpse-error mb-4">
                        <i class="bi bi-exclamation-circle me-2"></i><%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <form action="RegisterVideoServlet" method="POST">

                    <div class="mb-3">
                        <label class="form-label">Title</label>
                        <input type="text" name="title" class="form-control"
                               placeholder="My awesome video" required>
                    </div>

                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Duration</label>
                            <input type="time" name="duration" class="form-control"
                                   step="1" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Format</label>
                            <input type="text" name="format" class="form-control"
                                   placeholder="mp4, ogg…" required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Description</label>
                        <textarea name="description" class="form-control" rows="3"
                                  placeholder="What is this video about?"></textarea>
                    </div>

                    <div class="mb-4">
                        <label class="form-label">File path / URL</label>
                        <input type="text" name="filePath" class="form-control"
                               placeholder="http://example.com/video.mp4">
                    </div>

                    <div class="d-flex gap-3">
                        <button type="submit" class="btn-glimpse">
                            <i class="bi bi-upload me-2"></i>Publish Video
                        </button>
                        <a href="ListVideosServlet" class="btn-glimpse-outline"
                           style="padding: 0.7rem 1.5rem; line-height:1.5;">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>