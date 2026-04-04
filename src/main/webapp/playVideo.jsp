<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="model.Video"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse - Play</title>
    <%@include file="navbar.jsp"%>
    <style>
        /* ── Player page layout ──────────────────────────────────────────── */
        .player-wrap {
            background: #0F0E1A;
            border-radius: 16px;
            overflow: hidden;
            box-shadow: 0 8px 40px rgba(0,0,0,0.4);
        }

        .player-wrap video {
            width: 100%;
            max-height: 520px;
            display: block;
            background: #000;
        }

        .video-meta {
            background: #fff;
            border: 1.5px solid var(--glimpse-border);
            border-radius: 16px;
            padding: 1.5rem 2rem;
            margin-top: 1.25rem;
        }

        .video-title {
            font-family: 'Syne', sans-serif;
            font-size: 1.55rem;
            font-weight: 800;
            color: var(--glimpse-dark);
            margin: 0 0 0.5rem 0;
            line-height: 1.25;
        }

        .meta-row {
            display: flex;
            flex-wrap: wrap;
            gap: 1.25rem;
            color: var(--glimpse-muted);
            font-size: 0.88rem;
            margin-top: 0.5rem;
        }

        .meta-item {
            display: flex;
            align-items: center;
            gap: 5px;
        }

        .meta-item strong {
            color: var(--glimpse-text);
        }

        .views-badge {
            background: var(--glimpse-light);
            color: var(--glimpse-primary);
            padding: 3px 12px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.88rem;
        }

        .desc-block {
            margin-top: 1rem;
            padding-top: 1rem;
            border-top: 1px solid var(--glimpse-border);
            color: var(--glimpse-text);
            font-size: 0.95rem;
            line-height: 1.6;
        }

        .offline-warning {
            background: #FEF9C3;
            border: 1.5px solid #FDE047;
            color: #713F12;
            border-radius: 10px;
            padding: 0.7rem 1.1rem;
            font-size: 0.85rem;
            font-weight: 500;
            margin-top: 0.75rem;
            display: flex;
            align-items: center;
            gap: 8px;
        }
    </style>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        Video video        = (Video) request.getAttribute("video");
        Integer updatedViews = (Integer) request.getAttribute("updatedViews");

        if (video == null) {
            response.sendRedirect("ListVideosServlet");
            return;
        }

        // The src for the <video> element goes through ServeVideoServlet,
        // which supports HTTP Range requests so the browser can seek.
        String videoSrc = "ServeVideoServlet?file=" + video.getFilePath();

        // Display view count: use the REST-updated value if available,
        // fall back to the DB value if REST was offline.
        int displayViews = (updatedViews != null && updatedViews >= 0)
                           ? updatedViews
                           : video.getViews();

        boolean restOffline = (updatedViews == null || updatedViews < 0);
    %>

    <div class="container py-4">

        <%-- Breadcrumb --%>
        <div class="d-flex gap-3 align-items-center mb-3">
            <a href="ListVideosServlet"
               style="color:var(--glimpse-accent); font-size:0.9rem; font-weight:600;
                      text-decoration:none; display:inline-flex; align-items:center; gap:4px;">
                <i class="bi bi-arrow-left"></i> All videos
            </a>
            <span style="color:var(--glimpse-border);">|</span>
            <a href="search.jsp"
               style="color:var(--glimpse-accent); font-size:0.9rem; font-weight:600;
                      text-decoration:none; display:inline-flex; align-items:center; gap:4px;">
                <i class="bi bi-search"></i> Search
            </a>
        </div>

        <%-- Video player --%>
        <div class="player-wrap">
            <video controls autoplay>
                <source src="<%= videoSrc %>" type="video/<%= video.getFormat() %>">
                Your browser does not support the video tag.
            </video>
        </div>

        <%-- REST offline warning (shown below the player, not blocking playback) --%>
        <% if (restOffline) { %>
            <div class="offline-warning">
                <i class="bi bi-exclamation-triangle-fill"></i>
                The view count could not be updated because the search service
                (glimpse-rest) is currently offline. Playback is unaffected.
            </div>
        <% } %>

        <%-- Video metadata --%>
        <div class="video-meta">
            <h1 class="video-title"><%= video.getTitle() %></h1>

            <div class="meta-row">
                <div class="meta-item">
                    <i class="bi bi-person-circle"></i>
                    <strong><%= video.getAuthor() %></strong>
                </div>
                <div class="meta-item">
                    <i class="bi bi-calendar3"></i>
                    <%= video.getCreationDate() %>
                </div>
                <div class="meta-item">
                    <i class="bi bi-clock"></i>
                    <%= video.getDuration() %>
                </div>
                <div class="meta-item">
                    <span class="views-badge">
                        <i class="bi bi-play-fill"></i>
                        <%= displayViews %> view<%= displayViews == 1 ? "" : "s" %>
                    </span>
                </div>
                <div class="meta-item">
                    <span style="background:#F1F5F9; color:var(--glimpse-muted);
                                 padding:2px 8px; border-radius:4px;
                                 font-size:0.8rem; font-family:monospace;">
                        <%= video.getFormat().toUpperCase() %>
                    </span>
                </div>
            </div>

            <% if (video.getDescription() != null && !video.getDescription().isEmpty()) { %>
                <div class="desc-block">
                    <%= video.getDescription() %>
                </div>
            <% } %>
        </div>

    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
