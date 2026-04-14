<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="model.Video"%>
<%@page import="util.ViewUtils"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Glimpse - Play</title>

    <%-- Video.js v10 (HTML framework) --%>
    <script type="module" src="https://cdn.jsdelivr.net/npm/@videojs/html/cdn/video.js"></script>

    <%@include file="navbar.jsp"%>

    <style>
        .player-page {
            max-width: 960px;
            margin: 0 auto;
            padding: 1.5rem 1rem 4rem;
        }

        /* v10 container */
        .player-aspect {
            position: relative;
            width: 100%;
            aspect-ratio: 16/9;
        }

        .player-aspect video-player {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            --media-border-radius: 16px;
        }

        /* Video metadata card */
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
            margin-bottom: 0.8rem;
        }

        .meta-row {
            display: flex;
            flex-wrap: wrap;
            gap: 1rem;
            align-items: center;
            color: var(--glimpse-muted);
            font-size: 0.88rem;
        }

        .meta-item { display: flex; gap: 5px; align-items: center; }

        .views-badge {
            background: var(--glimpse-light);
            color: var(--glimpse-primary);
            padding: 3px 12px;
            border-radius: 20px;
            font-weight: 700;
        }

        .desc-block {
            margin-top: 1rem;
            padding-top: 1rem;
            border-top: 1px solid var(--glimpse-border);
        }

        /* REST offline warning */
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

    Video video = (Video) request.getAttribute("video");
    Integer updatedViews = (Integer) request.getAttribute("updatedViews");

    if (video == null) {
        response.sendRedirect("ListVideosServlet");
        return;
    }

    String videoSrc = request.getContextPath() + "/ServeVideoServlet?id=" + video.getId();

    // Derive a proper MIME type for the <source> tag
    String fmt = video.getFormat() != null ? video.getFormat().toLowerCase() : "";
    String mimeType;
    switch (fmt) {
        case "mp4":  mimeType = "video/mp4";       break;
        case "webm": mimeType = "video/webm";      break;
        case "ogv":
        case "ogg":  mimeType = "video/ogg";       break;
        case "mov":  mimeType = "video/quicktime";  break;
        case "avi":  mimeType = "video/x-msvideo"; break;
        case "mkv":  mimeType = "video/x-matroska"; break;
        case "mp3":  mimeType = "audio/mpeg";      break;
        case "wav":  mimeType = "audio/wav";       break;
        case "flac": mimeType = "audio/flac";      break;
        default:     mimeType = "video/mp4";       break;
    }

    int displayViews = (updatedViews != null && updatedViews >= 0)
        ? updatedViews : video.getViews();

    boolean restOffline = (updatedViews == null || updatedViews < 0);

    boolean userLiked = video.isUserLiked();
    int likeCount = video.getLikeCount();
%>

<div class="player-page">

    <!-- Video.js v10 player -->
    <div class="player-aspect">
        <video-player id="player">
            <video-skin>
                <video src="<%= ViewUtils.attr(videoSrc) %>"
                       type="<%= ViewUtils.attr(mimeType) %>"
                       playsinline
                       preload="metadata">
                </video>
            </video-skin>
        </video-player>
    </div>

    <%-- REST offline warning (below player, playback still works) --%>
    <% if (restOffline) { %>
    <div class="offline-warning">
        <i class="bi bi-exclamation-triangle-fill"></i>
        The view count could not be updated because the search service
        (glimpse-rest) is currently offline. Playback is unaffected.
    </div>
    <% } %>

    <%-- Video metadata card --%>
    <div class="video-meta">
        <h1 class="video-title"><%= ViewUtils.h(video.getTitle()) %></h1>

        <div class="meta-row">
            <div class="meta-item">
                <i class="bi bi-person-circle"></i>
                <strong style="color:var(--glimpse-text);">
                    <%= ViewUtils.h(video.getAuthor()) %>
                </strong>
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
                <i class="bi bi-file-earmark-play"></i>
                <%= ViewUtils.h(video.getFormat().toUpperCase()) %>
            </div>
            <div class="meta-item">
                <span class="views-badge">
                    <i class="bi bi-play-fill"></i>
                    <span id="view-count"><%= displayViews %></span>
                    view<%= displayViews == 1 ? "" : "s" %>
                </span>
            </div>

            <!-- AJAX Like -->
            <div style="margin-left:auto;">
                <button
                    class="btn-glimpse-like<%= userLiked ? " active" : "" %>"
                    id="like-btn-<%= video.getId() %>"
                    data-id="<%= video.getId() %>"
                    data-liked="<%= userLiked ? "true" : "false" %>"
                    onclick="toggleLike(this)">
                    <i class="bi bi-heart<%= userLiked ? "-fill" : "" %> me-1"
                       id="like-icon-<%= video.getId() %>"></i>
                    <span id="like-label-<%= video.getId() %>"><%= userLiked ? "Unlike" : "Like" %></span>
                    <span id="like-count-num-<%= video.getId() %>"><%= likeCount %></span>
                </button>
            </div>
        </div>

        <% if (video.getDescription() != null && !video.getDescription().isEmpty()) { %>
        <div class="desc-block">
            <%= ViewUtils.h(video.getDescription()) %>
        </div>
        <% } %>
    </div>
</div>

<script>
/* Keyboard shortcuts */
const video = document.querySelector("video");

document.addEventListener("keydown", e => {
    if (!video) return;

    switch(e.key) {
        case "k":
        case "K":
            video.paused ? video.play() : video.pause();
            break;
        case "ArrowRight":
            video.currentTime += 10;
            break;
        case "ArrowLeft":
            video.currentTime -= 10;
            break;
        case "m":
            video.muted = !video.muted;
            break;
        case "ArrowDown":
            e.preventDefault();
            video.volume = Math.max(video.volume - 0.1, 0);
            break;
        case "ArrowUp":
            e.preventDefault();
            video.volume = Math.min(video.volume + 0.1, 1);
            break;
        case "f":
        case "F":
            const player = document.querySelector("video-player");
            if (!document.fullscreenElement) {
                player.requestFullscreen();
            } else {
                document.exitFullscreen();
            }
            break;
    }
});

/* AJAX Like */
function toggleLike(btn) {
    const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";
    const videoId = btn.dataset.id;

    btn.disabled = true;

    fetch("LikeAjaxServlet", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "X-CSRF-Token": csrfToken
        },
        body: "id=" + encodeURIComponent(videoId)
    })
    .then(r => {
        if (!r.ok) throw new Error("HTTP " + r.status);
        return r.json();
    })
    .then(data => {
        const nowLiked = data.liked;
        const newCount = data.likeCount;

        // Update button state
        btn.dataset.liked = nowLiked ? "true" : "false";
        btn.classList.toggle("active", nowLiked);

        // Update icon
        const icon = document.getElementById("like-icon-" + videoId);
        if (icon) icon.className = nowLiked ? "bi bi-heart-fill me-1" : "bi bi-heart me-1";

        // Update label
        const label = document.getElementById("like-label-" + videoId);
        if (label) label.textContent = nowLiked ? "Unlike" : "Like";

        // Update count
        const countNum = document.getElementById("like-count-num-" + videoId);
        if (countNum) countNum.textContent = newCount;
    })
    .catch(() => { /* silent fail */ })
    .finally(() => { btn.disabled = false; });
}
</script>

</body>
</html>
