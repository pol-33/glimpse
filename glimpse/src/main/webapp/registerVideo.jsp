<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="security.Csrf"%>
<%@page import="util.ViewUtils"%>
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
                Publishing as <strong><%= ViewUtils.h(session.getAttribute("loggedUser")) %></strong>
            </p>

            <div class="glimpse-card">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-glimpse-error mb-4">
                        <i class="bi bi-exclamation-circle me-2"></i><%= ViewUtils.h(request.getAttribute("error")) %>
                    </div>
                <% } %>

                <form action="RegisterVideoServlet" method="POST"
                      enctype="multipart/form-data" id="videoForm">
                    <input type="hidden" name="csrfToken"
                           value="<%= ViewUtils.attr(Csrf.ensureToken(session)) %>">

                    <div class="mb-3">
                        <label class="form-label">Title</label>
                        <input type="text" name="title" class="form-control"
                               placeholder="My awesome video" required>
                    </div>

                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label" id="duration-label">Duration</label>
                            <input type="time" name="duration" id="duration"
                                   class="form-control" step="1"
                                   value="00:00:00" required>
                            <small id="duration-hint"
                                   style="color:var(--glimpse-danger); font-size:0.82rem; display:none;">
                                Duration cannot be 00:00:00.
                            </small>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" id="format-label">Format</label>
                            <input type="text" name="format" id="format"
                                   class="form-control" placeholder="mp4, ogg…" required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">
                            Description
                            <span id="desc-count"
                                  style="font-weight:400; color:var(--glimpse-muted);
                                         font-size:0.82rem; margin-left:6px;">
                                0 / 255
                            </span>
                        </label>
                        <textarea name="description" id="description"
                                  class="form-control" rows="3"
                                  maxlength="255"
                                  placeholder="What is this video about?"
                                  oninput="updateDescCount()"></textarea>
                    </div>

                    <div class="mb-4">
                        <label class="form-label">
                            Video file <span style="color:var(--glimpse-danger);">*</span>
                        </label>
                        <div class="d-flex gap-3 mb-3">
                            <label style="cursor:pointer; display:flex; align-items:center; gap:6px;">
                                <input type="radio" name="fileChoice" value="url"
                                       onchange="toggleFileChoice('url')" required>
                                <span style="font-size:0.9rem;">External URL</span>
                            </label>
                            <label style="cursor:pointer; display:flex; align-items:center; gap:6px;">
                                <input type="radio" name="fileChoice" value="upload"
                                       onchange="toggleFileChoice('upload')" required>
                                <span style="font-size:0.9rem;">Upload file</span>
                            </label>
                        </div>

                        <div id="section-url" style="display:none;">
                            <input type="text" name="fileUrl" class="form-control"
                                   placeholder="https://example.com/video.mp4">
                        </div>

                        <div id="section-upload" style="display:none;">
                            <input type="file" name="fileUpload" id="fileUpload"
                                   class="form-control" accept="video/*,audio/*"
                                   onchange="extractFileMetadata(this)">
                            <small style="color:var(--glimpse-muted); font-size:0.82rem;">
                                Max 500 MB
                            </small>
                            <video id="meta-reader" style="display:none;" preload="metadata"></video>
                        </div>
                    </div>

                    <div class="d-flex gap-3">
                        <button type="submit" class="btn-glimpse"
                                onclick="return validateForm()">
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

    <script>
        // ── Description character counter ────────────────────────────────────
        function updateDescCount() {
            const textarea = document.getElementById('description');
            const counter  = document.getElementById('desc-count');
            const len      = textarea.value.length;
            counter.textContent = len + ' / 255';
            counter.style.color = len > 255 * 0.9
                ? 'var(--glimpse-danger)'
                : 'var(--glimpse-muted)';
        }

        // ── Badge HTML (reused in both labels) ───────────────────────────────
        const AUTO_BADGE =
            '<span style="background:var(--glimpse-light);' +
            'color:var(--glimpse-primary); font-size:0.75rem;' +
            'padding:1px 7px; border-radius:20px; margin-left:6px;' +
            'font-weight:600;">auto-detected</span>';

        // ── File section toggle ──────────────────────────────────────────────
        function toggleFileChoice(choice) {
            document.getElementById('section-url').style.display =
                choice === 'url' ? 'block' : 'none';
            document.getElementById('section-upload').style.display =
                choice === 'upload' ? 'block' : 'none';

            // Always fully reset both fields and labels on every mode switch
            // so no stale auto-detected values survive toggling
            const durationInput = document.getElementById('duration');
            const formatInput   = document.getElementById('format');

            durationInput.value      = '00:00:00';
            formatInput.value        = '';
            durationInput.readOnly   = false;
            formatInput.readOnly     = false;
            durationInput.style.background = '';
            formatInput.style.background   = '';
            document.getElementById('duration-label').innerHTML = 'Duration';
            document.getElementById('format-label').innerHTML   = 'Format';
            document.getElementById('duration-hint').style.display = 'none';
            document.getElementById('fileUpload').value = '';

            if (choice === 'upload') {
                // Lock format immediately, as the server derives it from the filename.
                // Duration stays locked and will be auto-updated when the file is selected and metadata loads.
                formatInput.readOnly         = true;
                durationInput.readOnly       = true;
                formatInput.style.background    = 'var(--glimpse-light)';
                durationInput.style.background  = 'var(--glimpse-light)';
                formatInput.removeAttribute('required');
            } else {
                formatInput.setAttribute('required', '');
            }
        }

        // ── Auto-detect duration and format from uploaded file ───────────────
        function extractFileMetadata(input) {
            if (!input.files || !input.files[0]) return;

            const file      = input.files[0];
            const durationInput = document.getElementById('duration');
            const formatInput   = document.getElementById('format');

            // Lock duration immediately on file select —
            // unlocking only happens when the user switches back to URL
            durationInput.readOnly         = true;
            durationInput.style.background = 'var(--glimpse-light)';

            // Auto-detect format from MIME type (cosmetic — server re-derives it)
            const mimeType = file.type; // e.g. "video/mp4"
            const format   = mimeType.includes('/')
                ? mimeType.split('/')[1].split(';')[0]
                : '';
            if (format) {
                formatInput.value = format;
                document.getElementById('format-label').innerHTML =
                    'Format' + AUTO_BADGE;
            }

            // Auto-detect duration via hidden <video> element
            const videoEl   = document.getElementById('meta-reader');
            const objectUrl = URL.createObjectURL(file);
            videoEl.src     = objectUrl;

            videoEl.onloadedmetadata = function() {
                const totalSecs = Math.round(videoEl.duration);
                if (!isNaN(totalSecs) && totalSecs > 0) {
                    const h = Math.floor(totalSecs / 3600);
                    const m = Math.floor((totalSecs % 3600) / 60);
                    const s = totalSecs % 60;
                    durationInput.value =
                        String(h).padStart(2, '0') + ':' +
                        String(m).padStart(2, '0') + ':' +
                        String(s).padStart(2, '0');
                    document.getElementById('duration-label').innerHTML =
                        'Duration' + AUTO_BADGE;
                }
                URL.revokeObjectURL(objectUrl);
            };

            videoEl.onerror = function() {
                // Metadata unreadable - unlock duration so user can type it
                durationInput.readOnly         = false;
                durationInput.style.background = '';
                URL.revokeObjectURL(objectUrl);
            };
        }

        // ── Submit validation ────────────────────────────────────────────────
        function validateForm() {
            const duration = document.getElementById('duration').value;
            const hint     = document.getElementById('duration-hint');
            if (!duration || duration === '00:00:00') {
                hint.style.display = 'block';
                document.getElementById('duration').focus();
                return false;
            }
            hint.style.display = 'none';
            return true;
        }
        
        // Add listener
        window.addEventListener('DOMContentLoaded', function() {
            const firstOption = document.querySelector('input[name="fileChoice"][value="url"]');
            if (firstOption) {
                firstOption.checked = true;
                toggleFileChoice('url'); // show the corresponding section
            }
        });
    </script>
</body>
</html>
