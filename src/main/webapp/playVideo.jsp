<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="model.Video"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Glimpse - Play</title>

    <%-- Video.js core (latest stable) --%>
    <link  href="https://vjs.zencdn.net/8.23.4/video-js.min.css" rel="stylesheet">
    <script src="https://vjs.zencdn.net/8.23.4/video.min.js"></script>

    <%@include file="navbar.jsp"%>

    <style>
        /* Layout */
        .player-page {
            max-width: 960px;
            margin: 0 auto;
            padding: 2rem 1rem 4rem;
        }

        /* Video.js skin override (Glimpse colours) */
        .video-js {
            width: 100%;
            height: 0;
            padding-top: 56.25%; /* 16:9 */
            border-radius: 12px;
            overflow: hidden;
            background: #0F0E1A;
        }

        /* Remove the default big play button centering quirk */
        .vjs-poster { border-radius: 12px; }

        /* Progress bar accent colour */
        .video-js .vjs-play-progress,
        .video-js .vjs-volume-level {
            background-color: #6366F1;
        }

        /* Control bar */
        .video-js .vjs-control-bar {
            background: rgba(30, 27, 75, 0.88);
            border-radius: 0 0 12px 12px;
        }

        /* Big play button */
        .video-js .vjs-big-play-button {
            background-color: rgba(99, 102, 241, 0.82);
            border: 3px solid #fff;
            border-radius: 50%;
            width: 72px;
            height: 72px;
            line-height: 68px;
            font-size: 2rem;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            margin: 0;
        }
        .video-js:hover .vjs-big-play-button {
            background-color: rgba(99, 102, 241, 1);
        }

        /* Meta card */
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
            margin: 0 0 0.6rem 0;
            line-height: 1.25;
        }

        .meta-row {
            display: flex;
            flex-wrap: wrap;
            gap: 1.25rem;
            align-items: center;
            color: var(--glimpse-muted);
            font-size: 0.88rem;
        }

        .meta-item {
            display: flex;
            align-items: center;
            gap: 5px;
        }

        .views-badge {
            background: var(--glimpse-light);
            color: var(--glimpse-primary);
            padding: 3px 12px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.88rem;
        }

        .format-badge {
            background: #F1F5F9;
            color: var(--glimpse-muted);
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 0.8rem;
            font-family: monospace;
        }

        .desc-block {
            margin-top: 1rem;
            padding-top: 1rem;
            border-top: 1px solid var(--glimpse-border);
            color: var(--glimpse-text);
            font-size: 0.95rem;
            line-height: 1.6;
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

        Video video        = (Video) request.getAttribute("video");
        Integer updatedViews = (Integer) request.getAttribute("updatedViews");

        if (video == null) {
            response.sendRedirect("ListVideosServlet");
            return;
        }

        // ServeVideoServlet supports HTTP Range requests so Video.js can seek
        String videoSrc = "ServeVideoServlet?file=" + video.getFilePath();

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
                           ? updatedViews
                           : video.getViews();

        boolean restOffline = (updatedViews == null || updatedViews < 0);
    %>

    <div class="player-page">

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

        <%-- Video.js player --%>
        <video
            id="glimpse-player"
            class="video-js vjs-default-skin vjs-big-play-centered"
            controls
            preload="metadata"
            data-setup='{}'>
            <source src="<%= videoSrc %>" type="<%= mimeType %>">
            <p class="vjs-no-js">
                To view this video please enable JavaScript, and consider upgrading
                to a browser that supports HTML5 video.
            </p>
        </video>

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
            <h1 class="video-title"><%= video.getTitle() %></h1>
            <div class="meta-row">
                <div class="meta-item">
                    <i class="bi bi-person-circle"></i>
                    <strong style="color:var(--glimpse-text);">
                        <%= video.getAuthor() %>
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
                    <span class="views-badge">
                        <i class="bi bi-play-fill"></i>
                        <span id="view-count"><%= displayViews %></span>
                        view<%= displayViews == 1 ? "" : "s" %>
                    </span>
                </div>
                <div class="meta-item">
                    <span class="format-badge">
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

    <script>
        // Initialise Video.js with custom options
        var player = videojs('glimpse-player', {
            fluid: true,          // Fills the 16:9 wrapper
            playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 2],
            html5: {
                // Prefer native HLS on Safari, use MSE elsewhere.
                // Video.js HTTP Streaming (VHS) handles DASH/HLS if the
                // source type is application/dash+xml or application/x-mpegURL.
                // For uploaded MP4/WebM files we use standard progressive
                // download served with Range request support from ServeVideoServlet.
                nativeVideoTracks: false,
                nativeAudioTracks: false,
                nativeTextTracks:  false,
                vhs: {
                    overrideNative: true
                }
            },
            userActions: {
                // Double-click to enter/exit fullscreen
                doubleClick: true
            }
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', function(e) {
            // Ignore when typing in inputs
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
            switch (e.key) {
                case ' ':
                case 'k':
                    e.preventDefault();
                    player.paused() ? player.play() : player.pause();
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    player.currentTime(Math.min(player.currentTime() + 10, player.duration()));
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    player.currentTime(Math.max(player.currentTime() - 10, 0));
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    player.volume(Math.min(player.volume() + 0.1, 1));
                    break;
                case 'ArrowDown':
                    e.preventDefault();
                    player.volume(Math.max(player.volume() - 0.1, 0));
                    break;
                case 'f':
                    e.preventDefault();
                    player.isFullscreen() ? player.exitFullscreen() : player.requestFullscreen();
                    break;
                case 'm':
                    e.preventDefault();
                    player.muted(!player.muted());
                    break;
            }
        });
    </script>
</body>
</html>
